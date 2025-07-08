import uuid
import pandas as pd
import requests
import time
from tqdm import tqdm
from column_mapping import CSV_TO_QDRANT

# ====== 중복 컬럼 검사(CN_NM 기준) ======
def is_duplicate_cn_nm(cn_nm: str) -> bool:
    scroll_body = {
        "filter": {
            "must": [
                {"key": "CN_NM", "match": {"value": cn_nm}}
            ]
        },
        "limit": 1
    }

    try:
        res = requests.post(
            f"{QDRANT_URL}/collections/{COLLECTION_NAME}/points/scroll",
            json=scroll_body
        )
        res.raise_for_status()
        return len(res.json()["result"]["points"]) > 0
    except Exception as e:
        print(f"[ERROR] 중복 체크 실패 (CN_NM={cn_nm}): {e}")
        return False

# ====== 설정 ======
QDRANT_URL = "http://localhost:6333"
COLLECTION_NAME = "phishing_cases"
EMBEDDING_API_URL = "http://localhost:8000/v1/embed"
CSV_PATH = "articles.csv"

# ====== 1. CSV 파일 로딩 ======
df = pd.read_csv(CSV_PATH, encoding="utf-8")  # 필요 시 encoding="cp949"

# ====== 2. 텍스트 추출 (제목 + 부제목) ======
for _, row in tqdm(df.iterrows(), total=len(df)):
    cn_nm = row.get("컨텐츠명", "")
    if is_duplicate_cn_nm(cn_nm):
        print(f"[SKIP] 중복된 컨텐츠명: {cn_nm}")
        continue

    title = row.get("제목", "")
    subtitle = row.get("부제목", "")
    text = f"{title} {subtitle}".strip()

    # ====== 3. 임베딩 요청 ======
    try:
        embed_response = requests.post(EMBEDDING_API_URL, json={"text": text})
        embed_response.raise_for_status()
        vector = embed_response.json()["vector"]
    except Exception as e:
        print(f"[ERROR] 임베딩 실패 (컨텐츠명={row.get('컨텐츠명', '')}): {e}")
        continue

    # ====== 4. payload 구성 (한글 컬럼 → 영문 매핑) ======
    payload = {
        CSV_TO_QDRANT[k]: row[k] if pd.notna(row[k]) else ""
        for k in CSV_TO_QDRANT if k in row
    }

    # ✅ Qdrant 검색 일관성을 위한 필드 추가
    payload["source"] = "csv-upload"

    # ====== 5. Qdrant 저장 ======
    qdrant_body = {
        "points": [
            {
                "id": str(uuid.uuid4()),
                "vector": vector,
                "payload": payload
            }
        ]
    }

    try:
        res = requests.put(f"{QDRANT_URL}/collections/{COLLECTION_NAME}/points?wait=true", json=qdrant_body)
        res.raise_for_status()
    except requests.exceptions.HTTPError as e:
        print(f"[ERROR] Qdrant 저장 실패 (컨텐츠명={row.get('컨텐츠명', '')})")
        print(f"[STATUS CODE] {res.status_code}")
        print(f"[RESPONSE] {res.text}")


    time.sleep(0.01)


print("✅ 모든 데이터 업로드 완료.")
