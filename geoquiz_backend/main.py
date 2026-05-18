from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
import numpy as np
import json
import os
import tempfile
from typing import Dict, Any

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

DB_FILE = "faces_db.json"


def load_db() -> Dict[str, Any]:
    if not os.path.exists(DB_FILE):
        return {}
    with open(DB_FILE, "r", encoding="utf-8") as f:
        try:
            return json.load(f)
        except json.JSONDecodeError:
            return {}


def save_db(data: Dict[str, Any]) -> None:
    with open(DB_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)


def save_upload_to_temp(file: UploadFile) -> str:
    suffix = os.path.splitext(file.filename)[1] if file.filename else ".jpg"
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        tmp.write(file.file.read())
        return tmp.name


@app.post("/register-face")
async def register_face(user_id: str = Form(...), image: UploadFile = File(...)):
    from deepface import DeepFace
    tmp_path = save_upload_to_temp(image)
    try:
        result = DeepFace.represent(img_path=tmp_path, model_name="Facenet", enforce_detection=True)
        encoding = result[0]["embedding"]
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"No face detected or error: {e}")
    finally:
        os.remove(tmp_path)

    db = load_db()
    db[user_id] = encoding
    save_db(db)
    return {"status": "registered"}


@app.post("/verify-face")
async def verify_face(user_id: str = Form(...), image: UploadFile = File(...)):
    from deepface import DeepFace
    db = load_db()
    if user_id not in db:
        raise HTTPException(status_code=400, detail="No stored face for this user.")

    tmp_path = save_upload_to_temp(image)
    try:
        result = DeepFace.represent(img_path=tmp_path, model_name="Facenet", enforce_detection=True)
        incoming = np.array(result[0]["embedding"])
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"No face detected or error: {e}")
    finally:
        os.remove(tmp_path)

    stored = np.array(db[user_id])
    distance = np.linalg.norm(incoming - stored)
    # Facenet threshold: distance < 10 is typically a match
    match = bool(distance < 10)
    return {"match": match, "distance": round(float(distance), 4)}