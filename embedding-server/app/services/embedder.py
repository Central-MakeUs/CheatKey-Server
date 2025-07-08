from transformers import AutoTokenizer, AutoModel
import torch

class KoSimCSEEmbedder:
    def __init__(self, model_name: str = "jhgan/ko-sbert-nli"):
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModel.from_pretrained(model_name)

    def get_embedding(self, text: str) -> list[float]:
        inputs = self.tokenizer(text, return_tensors="pt", truncation=True, max_length=128)
        with torch.no_grad():
            outputs = self.model(**inputs, return_dict=True)
        embedding = outputs.pooler_output[0]  # [CLS] 임베딩
        return embedding.tolist()
