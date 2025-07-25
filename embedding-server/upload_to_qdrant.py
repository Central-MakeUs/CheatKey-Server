import uuid
import pandas as pd
import requests
import time
from tqdm import tqdm
from column_mapping import CSV_TO_QDRANT

# ====== 설정 ======
QDRANT_URL = "http://localhost:6333"
COLLECTION_NAME = "phishing_cases"
EMBEDDING_API_URL = "http://localhost:8000/v1/embed"
EXCEL_PATH = "articles.xlsx"

# ====== 중복 컬럼 검사 (CONTENT 기준) ======
def is_duplicate_contents(contents: str) -> bool:
    scroll_body = {
        "filter": {
            "must": [{"key": "CONTENT", "match": {"value": contents}}]
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
        print(f"[ERROR] 중복 체크 실패 (CONTENT={contents}): {e}")
        return False

# ====== 1. 엑셀 파일 로딩 ======
df = pd.read_excel(EXCEL_PATH)

for _, row in tqdm(df.iterrows(), total=len(df)):
    contents = row.get("내용", "")
    if not contents or pd.isna(contents) or contents.strip() == "":
        print(f"[SKIP] 내용 없음 - row={row}")
        continue
    contents = contents.strip()

    if is_duplicate_contents(contents):
        print(f"[SKIP] 중복된 컨텐츠: {contents}")
        continue

    category = row.get("카테고리", "").strip()
    text = f"{category} {contents}"

    # ====== 2. 임베딩 요청 ======
    try:
        embed_response = requests.post(EMBEDDING_API_URL, json={"text": text})
        embed_response.raise_for_status()
        vector = embed_response.json()["vector"]
    except Exception as e:
        print(f"[ERROR] 임베딩 실패 (CONTENT={contents}): {e}")
        continue

    # ====== 3. Payload 구성 ======
    payload = {
        CSV_TO_QDRANT[k]: str(row[k]).strip()
        for k in CSV_TO_QDRANT if k in row and pd.notna(row[k])
    }
    payload["CONTENT"] = contents
    payload["source"] = "csv-upload"

    # ====== 4. Qdrant 저장 ======
    qdrant_body = {
        "points": [{
            "id": str(uuid.uuid4()),
            "vector": vector,
            "payload": payload
        }]
    }

    try:
        res = requests.put(
            f"{QDRANT_URL}/collections/{COLLECTION_NAME}/points?wait=true",
            json=qdrant_body
        )
        res.raise_for_status()
    except requests.exceptions.HTTPError as e:
        print(f"[ERROR] Qdrant 저장 실패 (CONTENT={contents})")
        print(f"[STATUS CODE] {res.status_code}")
        print(f"[RESPONSE] {res.text}")

    time.sleep(0.01)

print("✅ 모든 데이터 업로드 완료.")
