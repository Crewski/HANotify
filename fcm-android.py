"""
FCM Android notification service.

"""

import json
import logging
import requests

from aiohttp.hdrs import AUTHORIZATION
import voluptuous as vol
from voluptuous.humanize import humanize_error

from homeassistant.util.json import load_json, save_json
from homeassistant.exceptions import HomeAssistantError
from homeassistant.components.frontend import add_manifest_json_key
from homeassistant.components.http import HomeAssistantView
from homeassistant.components.notify import (
    ATTR_DATA, ATTR_TITLE, ATTR_TARGET, PLATFORM_SCHEMA, ATTR_TITLE_DEFAULT,
    BaseNotificationService)
from homeassistant.const import (
    URL_ROOT, HTTP_BAD_REQUEST, HTTP_UNAUTHORIZED, HTTP_INTERNAL_SERVER_ERROR)
from homeassistant.helpers import config_validation as cv
from homeassistant.util import ensure_unique_string


DEPENDENCIES = ['frontend']

_LOGGER = logging.getLogger(__name__)

REGISTRATIONS_FILE = 'fcm_android_registrations.conf'


API_KEY = 'AIzaSyDIGxzoJksF9b2ifmJmkuCzoMnp6YdYcX8'
API_KEY_HEADER = 'key=' + API_KEY
ATTR_TOKEN = 'token'
FCM_POST_URL = 'https://fcm.googleapis.com/fcm/send'

ATTR_TITLE = 'title'
ATTR_COLOR = 'color'


ATTR_ACTION = 'action'
ATTR_ACTIONS = 'actions'
ATTR_TYPE = 'type'



REGISTER_SCHEMA = vol.Schema({
    vol.Required(ATTR_TOKEN): cv.string,
})

CALLBACK_EVENT_PAYLOAD_SCHEMA = vol.Schema({
    vol.Required(ATTR_TYPE): vol.In(['clicked']),
    vol.Required(ATTR_TOKEN): cv.string,
    vol.Optional(ATTR_ACTION): cv.string,
    vol.Optional(ATTR_DATA): dict,
})

NOTIFY_CALLBACK_EVENT = 'fcm_android_notifications'


def get_service(hass, config, discovery_info=None):
    """Get the FCM Android push notification service."""
    json_path = hass.config.path(REGISTRATIONS_FILE)

    registrations = _load_config(json_path)

    if registrations is None:
        return None

    hass.http.register_view(
        FCMAndroidRegistrationView(registrations, json_path))
    hass.http.register_view(FCMAndroidCallbackView(registrations))

    return FCMAndroidNotificationService(registrations, json_path)


def _load_config(filename):
    """Load configuration."""
    try:
        return load_json(filename)
    except HomeAssistantError:
        pass
    return {}


class JSONBytesDecoder(json.JSONEncoder):
    """JSONEncoder to decode bytes objects to unicode."""

    # pylint: disable=method-hidden, arguments-differ
    def default(self, obj):
        """Decode object if it's a bytes object, else defer to base class."""
        if isinstance(obj, bytes):
            return obj.decode()
        return json.JSONEncoder.default(self, obj)


class FCMAndroidRegistrationView(HomeAssistantView):
    """Accepts push registrations from android."""

    url = '/api/notify.fcm-android'
    name = 'api:notify.fcm-android'

    def __init__(self, registrations, json_path):
        """Init HTML5PushRegistrationView."""
        self.registrations = registrations
        self.json_path = json_path

    async def post(self, request):
        """Accept the POST request for push registrations from Android."""
        try:
            data = await request.json()
        except ValueError:
            return self.json_message('Invalid JSON', HTTP_BAD_REQUEST)

        try:
            data = REGISTER_SCHEMA(data)
        except vol.Invalid as ex:
            return self.json_message(
                humanize_error(data, ex), HTTP_BAD_REQUEST)

        name = self.find_registration_name(data)
        previous_registration = self.registrations.get(name)

        self.registrations[name] = data

        try:
            hass = request.app['hass']

            await hass.async_add_job(save_json, self.json_path,
                                     self.registrations)
            return self.json_message(
                'Push notification subscriber registered.')
        except HomeAssistantError:
            if previous_registration is not None:
                self.registrations[name] = previous_registration
            else:
                self.registrations.pop(name)

            return self.json_message(
                'Error saving registration.', HTTP_INTERNAL_SERVER_ERROR)

    def find_registration_name(self, data):
        """Find a registration name matching data or generate a unique one."""
        token = data.get(ATTR_TOKEN)
        for key, registration in self.registrations.items():
            if registration.get(ATTR_TOKEN) == token:
                return key
        return ensure_unique_string('unnamed device', self.registrations)

    async def delete(self, request):
        """Delete a registration."""
        try:
            data = await request.json()
        except ValueError:
            return self.json_message('Invalid JSON', HTTP_BAD_REQUEST)
            
        token = data.get(ATTR_TOKEN)

        found = None

        for key, registration in self.registrations.items():
            if registration.get(ATTR_TOKEN) == token:
                found = key
                break

        if not found:
            # If not found, unregistering was already done. Return 200
            return self.json_message('Registration not found.')

        reg = self.registrations.pop(found)

        try:
            hass = request.app['hass']

            await hass.async_add_job(save_json, self.json_path,
                                     self.registrations)
        except HomeAssistantError:
            self.registrations[found] = reg
            return self.json_message(
                'Error saving registration.', HTTP_INTERNAL_SERVER_ERROR)

        return self.json_message('Push notification subscriber unregistered.')


class FCMAndroidCallbackView(HomeAssistantView):
    """Accepts notification callback from Android."""

    requires_auth = False
    url = '/api/notify.fcm-android/callback'
    name = 'api:notify.fcm-android/callback'

    def __init__(self, registrations):
        """Init FCMAndroidCallbackView."""
        self.registrations = registrations

    async def post(self, request):
        """Accept the POST request for push registrations event callback."""

        try:
            data = await request.json()
        except ValueError:
            return self.json_message('Invalid JSON', HTTP_BAD_REQUEST)

        found = None
        for key, registration in self.registrations.items():
            if registration.get(ATTR_TOKEN) == data[ATTR_TOKEN]:
                found = key
                break
        
        if not found:
            _LOGGER.error('Callback not from registered device')
            return self.json_message('Callback received from invalid device')

        event_payload = {
            ATTR_TYPE: data[ATTR_TYPE],            
        }

        if data[ATTR_ACTION] is not None:
            event_payload[ATTR_ACTION] = data[ATTR_ACTION]

        if data.get(ATTR_DATA) is not None:
            event_payload[ATTR_DATA] = data.get(ATTR_DATA)

        try:
            event_payload = CALLBACK_EVENT_PAYLOAD_SCHEMA(event_payload)
        except vol.Invalid as ex:
            _LOGGER.warning("Callback event payload is not valid: %s",
                            humanize_error(event_payload, ex))

        event_name = '{}.{}'.format(NOTIFY_CALLBACK_EVENT,
                                    event_payload[ATTR_TYPE])
        request.app['hass'].bus.fire(event_name, event_payload)
        return self.json({'status': 'ok', 'event': event_payload[ATTR_TYPE]})


class FCMAndroidNotificationService(BaseNotificationService):
    """Implement the notification service for HTML5."""

    def __init__(self, registrations, json_path):
        """Initialize the service."""
        self.registrations = registrations
        self.registrations_json_path = json_path

    @property
    def targets(self):
        """Return a dictionary of registered targets."""
        targets = {}
        for registration in self.registrations:
            targets[registration] = registration
        return targets

    def send_message(self, message="", **kwargs):
        """Send a message to a user."""
        headers = {
            'Authorization': API_KEY_HEADER,
            'Content-Type': 'application/json'
        }

        payload = {
            'data': {
                'body': message,
                ATTR_TITLE: kwargs.get(ATTR_TITLE, ATTR_TITLE_DEFAULT),
                'content_available': False,
                'color': '#50C0F2',
                ATTR_ACTIONS: []
            }
        }

        data = kwargs.get(ATTR_DATA)
        
        if data is not None:
            if (data.get(ATTR_ACTIONS)) is not None:
                payload[ATTR_DATA][ATTR_ACTIONS] = data.get(ATTR_ACTIONS)
            if data.get(ATTR_COLOR) is not None:
                payload[ATTR_DATA][ATTR_COLOR] = data.get(ATTR_COLOR)

        targets = kwargs.get(ATTR_TARGET)
        target_tmp = []

        if not targets:
            targets = self.registrations.keys()            

        for target in list(targets):
            info = self.registrations.get(target)
            if info is None:
                _LOGGER.error("%s is not a valid HTML5 push notification target", target)
                continue
            target_tmp.append(info[ATTR_TOKEN])

        payload['registration_ids'] = target_tmp

        response = requests.post(FCM_POST_URL, headers=headers,
                                     json=payload, timeout=10)

        if response.status_code not in (200, 201):
            _LOGGER.exception(
                "Error sending message. Response %d: %s:",
                response.status_code, response.reason)
