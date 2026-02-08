from sentence_transformers import SentenceTransformer

model = SentenceTransformer("intfloat/multilingual-e5-large")

model.save("models/multilingual-e5-large")

print("모델 다운로드 완료")