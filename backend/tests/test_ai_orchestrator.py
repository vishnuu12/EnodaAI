import asyncio

import pytest

from app.services.ai_orchestrator import AIOrchestrator
from app.services.ai_service import AIService, AIServiceError


class StubService(AIService):
    """Configurable stub: gives a canned reply or fails on demand.
    Records the language each call received."""

    def __init__(self, name: str, reply: str | None):
        self.name = name
        self._reply = reply
        self.received_language: str | None = None

    async def generate_reply(self, message: str, language: str = "auto") -> str:
        self.received_language = language
        if self._reply is None:
            raise AIServiceError(f"{self.name} unavailable")
        return self._reply


def test_falls_back_to_second_provider():
    chain = AIOrchestrator(
        [
            StubService("primary", None),
            StubService("backup", "backup reply"),
        ]
    )

    reply = asyncio.run(chain.generate_reply("hi", "ta"))

    assert reply == "backup reply"


def test_raises_when_all_providers_fail():
    chain = AIOrchestrator(
        [
            StubService("primary", None),
            StubService("backup", None),
        ]
    )

    with pytest.raises(AIServiceError):
        asyncio.run(chain.generate_reply("hi"))


def test_first_provider_wins_when_healthy():
    chain = AIOrchestrator(
        [
            StubService("primary", "primary reply"),
            StubService("backup", "backup reply"),
        ]
    )

    reply = asyncio.run(chain.generate_reply("hi"))

    assert reply == "primary reply"


def test_language_reaches_the_provider():
    stub = StubService("primary", "ok")
    chain = AIOrchestrator([stub])

    asyncio.run(chain.generate_reply("hi", "ta"))

    assert stub.received_language == "ta"
