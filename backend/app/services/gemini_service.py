import logging

from google import genai
from google.genai import types

from app.services.ai_service import AIService, AIServiceError, build_system_prompt

logger = logging.getLogger("enodaai.ai.gemini")


class GeminiService(AIService):
    """Primary provider: Google Gemini free tier."""

    name = "gemini"

    def __init__(self, api_key: str, model: str):
        self._client = genai.Client(api_key=api_key)
        self._model = model

    async def generate_reply(self, message: str, language: str = "auto") -> str:
        try:
            response = await self._client.aio.models.generate_content(
                model=self._model,
                contents=message,
                config=types.GenerateContentConfig(
                    system_instruction=build_system_prompt(language)
                ),
            )
        except Exception as exc:
            # Any native Google/network error becomes AIServiceError
            # so the orchestrator can fall back to the next provider.
            raise AIServiceError(f"gemini request failed: {exc}") from exc

        text = (response.text or "").strip()
        if not text:
            raise AIServiceError("gemini returned an empty response")
        return text
