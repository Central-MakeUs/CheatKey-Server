from fastapi import APIRouter
from pydantic import BaseModel
from app.services.embedder import KoSimCSEEmbedder

router = APIRouter()
embedder = KoSimCSEEmbedder()

class TextRequest(BaseModel):
    text: str

class EmbeddingResponse(BaseModel):
    vector: list[float]

@router.post("/embed", response_model=EmbeddingResponse)
def embed_text(req: TextRequest):
    vector = embedder.get_embedding(req.text)
    return EmbeddingResponse(vector=vector)
