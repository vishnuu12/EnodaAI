from fastapi.testclient import TestClient

from app.core.config import get_settings
from app.main import app
from app.services.ai_orchestrator import AIOrchestrator, get_ai_orchestrator
from app.services.ai_service import AIService, AIServiceError

settings = get_settings()
client = TestClient(app)

CHAT_URL = f"{settings.api_v1_prefix}/chat"


class FakeAIService(AIService):
    """Never touches a real provider - replies with whatever we want,
    or simulates an outage by raising. Records the language it
    was asked for so tests can assert on it."""

    name = "fake"

    def __init__(self, reply: str | None):
        self._reply = reply
        self.received_language: str | None = None

    async def generate_reply(self, message: str, language: str = "auto") -> str:
        self.received_language = language
        if self._reply is None:
            raise AIServiceError("simulated provider outage")
        return self._reply


def _use_fake(reply: str | None) -> FakeAIService:
    fake = FakeAIService(reply)
    app.dependency_overrides[get_ai_orchestrator] = lambda: AIOrchestrator(
        [fake]
    )
    return fake


def teardown_function():
    app.dependency_overrides.clear()


def test_chat_returns_ai_reply():
    _use_fake("Hello! How can I help you today?")

    response = client.post(CHAT_URL, json={"text": "hi"})

    assert response.status_code == 200
    assert response.json()["reply"] == "Hello! How can I help you today?"


def test_chat_rejects_empty_text():
    response = client.post(CHAT_URL, json={"text": ""})

    assert response.status_code == 422


def test_chat_rejects_missing_text():
    response = client.post(CHAT_URL, json={})

    assert response.status_code == 422


def test_chat_returns_503_when_all_providers_fail():
    _use_fake(None)

    response = client.post(CHAT_URL, json={"text": "hi"})

    assert response.status_code == 503


def test_tamil_script_resolves_to_tamil():
    fake = _use_fake("வணக்கம்")

    response = client.post(CHAT_URL, json={"text": "நீ எப்படி இருக்கிறாய்?"})

    assert response.status_code == 200
    assert fake.received_language == "ta"


def test_english_hint_resolves_to_english():
    fake = _use_fake("Hello")

    client.post(CHAT_URL, json={"text": "hello there", "language": "en-IN"})

    assert fake.received_language == "en"


def test_no_hint_latin_is_auto():
    fake = _use_fake("Hello")

    client.post(CHAT_URL, json={"text": "hello there"})

    assert fake.received_language == "auto"
