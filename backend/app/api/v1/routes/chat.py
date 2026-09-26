from fastapi import APIRouter, Depends, HTTPException

from app.schemas.chat import ChatRequest, ChatResponse
from app.services.ai_orchestrator import AIOrchestrator, get_ai_orchestrator
from app.services.ai_service import AIServiceError
from app.services.language_service import resolve_language

router = APIRouter(tags=["chat"])


@router.post("/chat", response_model=ChatResponse)
async def chat(
    payload: ChatRequest,
    orchestrator: AIOrchestrator = Depends(get_ai_orchestrator),
) -> ChatResponse:
    """The assistant: recognized speech in, AI reply out.

    The reply language is resolved from the message's script and
    the app's mode hint: Tamil script -> Tamil; otherwise the
    hint decides; with no hint the LLM matches the user.

    503 means every provider failed - an upstream problem, not
    the client's fault.
    """
    language = resolve_language(payload.text, payload.language)
    try:
        reply = await orchestrator.generate_reply(payload.text, language)
    except AIServiceError as exc:
        raise HTTPException(status_code=503, detail=str(exc)) from exc
    return ChatResponse(reply=reply)
