import logging
from functools import lru_cache

from app.core.config import get_settings
from app.services.ai_service import AIService, AIServiceError
from app.services.conversation_memory import ConversationMessage
from app.services.echo_service import EchoService
from app.services.gemini_service import GeminiService
from app.services.groq_service import GroqService

logger = logging.getLogger("enodaai.ai")


class AIOrchestrator:
    """Tries each provider in order; first success wins.

    The fallback chain: gemini -> groq -> echo.

    Conversation history is passed to each provider so the AI
    can understand previous messages in the same conversation.
    """

    def __init__(self, services: list[AIService]):
        self._services = services

    async def generate_reply(
        self,
        message: str,
        language: str = "auto",
        history: list[ConversationMessage] | None = None,
    ) -> str:

        if not self._services:
            raise AIServiceError("no AI services configured")

        history = history or []

        last_error: AIServiceError | None = None

        for service in self._services:
            try:
                reply = await service.generate_reply(
                    message=message,
                    language=language,
                    history=history,
                )

                logger.info(
                    "ai_provider=%s served the request (language=%s)",
                    service.name,
                    language,
                )

                return reply

            except AIServiceError as exc:
                logger.warning(
                    "ai_provider=%s failed: %s - trying next provider",
                    service.name,
                    exc,
                )

                last_error = exc

        raise AIServiceError(
            f"all providers failed; last error: {last_error}"
        )


@lru_cache
def get_ai_orchestrator() -> AIOrchestrator:
    """Build the provider chain from settings, once per process.

    FastAPI injects this into the chat route via Depends; tests
    override it via app.dependency_overrides.
    """

    settings = get_settings()

    services: list[AIService] = []

    if settings.gemini_api_key:
        services.append(
            GeminiService(
                settings.gemini_api_key,
                settings.gemini_model
            )
        )

    if settings.groq_api_key:
        services.append(
            GroqService(
                settings.groq_api_key,
                settings.groq_model
            )
        )

    if not services:
        services.append(EchoService())

    return AIOrchestrator(services)