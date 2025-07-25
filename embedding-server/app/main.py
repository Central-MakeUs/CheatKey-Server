# FastAPI 실행 엔트리 포인트
from fastapi import FastAPI
from app.api.v1.endpoints import router

app = FastAPI()
app.include_router(router, prefix="/v1")
