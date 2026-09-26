from app.services.ai_service import AIService


class EchoService(AIService):
    """Last-resort fallback when NO provider keys are configured.

    Keeps the whole pipeline testable and demonstrable without any
    external account. The orchestrator only adds this when both
    gemini and groq keys are missing.
    """

    name = "echo"

    async def generate_reply(self, message: str, language: str = "auto") -> str:
        return "(echo mode - no AI key configured) You said: " + message
