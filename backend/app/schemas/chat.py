from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    """What the Android app sends after speech recognition."""

    text: str = Field(min_length=1, max_length=500)
    # Reserved for Phase 9 (language detection) - optional for now.
    language: str | None = Field(default=None, max_length=10)


class ChatResponse(BaseModel):
    """What the app displays (and later speaks) back."""

    reply: str
