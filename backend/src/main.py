from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime
import os
from geoalchemy2 import Geometry
from geoalchemy2.shape import to_shape
from shapely.geometry import Point

app = FastAPI(title="Taganrog Platform API", version="1.0.0")

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Database
DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://taganrog_user:taganrog_pass@db:5432/taganrog_db")
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(bind=engine)


def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


# Pydantic Models
class InitiativeCreate(BaseModel):
    title: str
    description: Optional[str] = ""
    status: str = "RED"  # RED, YELLOW, GREEN
    category: Optional[str] = ""
    lat: float
    lon: float
    author_id: Optional[str] = ""
    author_name: Optional[str] = "Гражданин"
    author_role: Optional[str] = "Гражданин"


class InitiativeResponse(BaseModel):
    id: int
    title: str
    description: str
    status: str
    category: str
    address: str
    lat: float
    lon: float
    author_name: str
    author_role: str
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


@app.get("/")
async def root():
    return {"message": "Taganrog Platform API", "version": "1.0.0"}


@app.get("/health")
async def health():
    return {"status": "healthy"}


@app.get("/initiatives", response_model=List[InitiativeResponse])
async def get_initiatives(
    status: Optional[str] = None,
    category: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Получить все инициативы с возможностью фильтрации"""
    query = """
        SELECT 
            id, title, description, status, category, address,
            ST_Y(geometry::geometry) as lat,
            ST_X(geometry::geometry) as lon,
            author_name, author_role, created_at, updated_at
        FROM initiatives
        WHERE 1=1
    """
    params = {}
    
    if status:
        query += " AND status = :status"
        params["status"] = status
    
    if category:
        query += " AND category = :category"
        params["category"] = category
    
    query += " ORDER BY created_at DESC"
    
    result = db.execute(text(query), params)
    rows = result.fetchall()
    
    initiatives = []
    for row in rows:
        initiatives.append(InitiativeResponse(
            id=row.id,
            title=row.title,
            description=row.description or "",
            status=row.status,
            category=row.category or "",
            address=row.address or "",
            lat=float(row.lat),
            lon=float(row.lon),
            author_name=row.author_name or "Гражданин",
            author_role=row.author_role or "Гражданин",
            created_at=row.created_at,
            updated_at=row.updated_at
        ))
    
    return initiatives


@app.get("/initiatives/{initiative_id}", response_model=InitiativeResponse)
async def get_initiative(initiative_id: int, db: Session = Depends(get_db)):
    """Получить инициативу по ID"""
    query = """
        SELECT 
            id, title, description, status, category, address,
            ST_Y(geometry::geometry) as lat,
            ST_X(geometry::geometry) as lon,
            author_name, author_role, created_at, updated_at
        FROM initiatives
        WHERE id = :id
    """
    
    result = db.execute(text(query), {"id": initiative_id})
    row = result.fetchone()
    
    if not row:
        raise HTTPException(status_code=404, detail="Initiative not found")
    
    return InitiativeResponse(
        id=row.id,
        title=row.title,
        description=row.description or "",
        status=row.status,
        category=row.category or "",
        address=row.address or "",
        lat=float(row.lat),
        lon=float(row.lon),
        author_name=row.author_name or "Гражданин",
        author_role=row.author_role or "Гражданин",
        created_at=row.created_at,
        updated_at=row.updated_at
    )


@app.post("/initiatives", response_model=InitiativeResponse)
async def create_initiative(initiative: InitiativeCreate, db: Session = Depends(get_db)):
    """Создать новую инициативу"""
    # Валидация статуса
    if initiative.status not in ["RED", "YELLOW", "GREEN"]:
        raise HTTPException(status_code=400, detail="Status must be RED, YELLOW, or GREEN")
    
    # Определение адреса через PostGIS функцию
    address_query = """
        SELECT get_street_name(:lat, :lon) as street_name
    """
    address_result = db.execute(text(address_query), {
        "lat": initiative.lat,
        "lon": initiative.lon
    })
    street_name = address_result.fetchone().street_name
    
    # Формирование адреса
    address = street_name if street_name != "Неизвестная улица" else ""
    
    # Вставка инициативы
    insert_query = """
        INSERT INTO initiatives (title, description, status, category, address, geometry, author_id, author_name, author_role)
        VALUES (:title, :description, :status, :category, :address, 
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 
                :author_id, :author_name, :author_role)
        RETURNING id, created_at, updated_at
    """
    
    result = db.execute(text(insert_query), {
        "title": initiative.title,
        "description": initiative.description or "",
        "status": initiative.status,
        "category": initiative.category or "",
        "address": address,
        "lat": initiative.lat,
        "lon": initiative.lon,
        "author_id": initiative.author_id or "",
        "author_name": initiative.author_name or "Гражданин",
        "author_role": initiative.author_role or "Гражданин"
    })
    
    db.commit()
    
    row = result.fetchone()
    
    # Получаем созданную инициативу
    return await get_initiative(row.id, db)


@app.put("/initiatives/{initiative_id}", response_model=InitiativeResponse)
async def update_initiative(
    initiative_id: int,
    initiative: InitiativeCreate,
    db: Session = Depends(get_db)
):
    """Обновить инициативу"""
    # Проверка существования
    existing = await get_initiative(initiative_id, db)
    
    # Валидация статуса
    if initiative.status not in ["RED", "YELLOW", "GREEN"]:
        raise HTTPException(status_code=400, detail="Status must be RED, YELLOW, or GREEN")
    
    # Определение адреса
    address_query = """
        SELECT get_street_name(:lat, :lon) as street_name
    """
    address_result = db.execute(text(address_query), {
        "lat": initiative.lat,
        "lon": initiative.lon
    })
    street_name = address_result.fetchone().street_name
    address = street_name if street_name != "Неизвестная улица" else ""
    
    # Обновление
    update_query = """
        UPDATE initiatives
        SET title = :title,
            description = :description,
            status = :status,
            category = :category,
            address = :address,
            geometry = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326),
            author_name = :author_name,
            author_role = :author_role
        WHERE id = :id
    """
    
    db.execute(text(update_query), {
        "id": initiative_id,
        "title": initiative.title,
        "description": initiative.description or "",
        "status": initiative.status,
        "category": initiative.category or "",
        "address": address,
        "lat": initiative.lat,
        "lon": initiative.lon,
        "author_name": initiative.author_name or "Гражданин",
        "author_role": initiative.author_role or "Гражданин"
    })
    
    db.commit()
    
    return await get_initiative(initiative_id, db)


@app.delete("/initiatives/{initiative_id}")
async def delete_initiative(initiative_id: int, db: Session = Depends(get_db)):
    """Удалить инициативу"""
    # Проверка существования
    await get_initiative(initiative_id, db)
    
    delete_query = "DELETE FROM initiatives WHERE id = :id"
    db.execute(text(delete_query), {"id": initiative_id})
    db.commit()
    
    return {"message": "Initiative deleted successfully"}


@app.get("/initiatives/geojson")
async def get_initiatives_geojson(
    status: Optional[str] = None,
    db: Session = Depends(get_db)
):
    """Получить инициативы в формате GeoJSON для карты"""
    query = """
        SELECT 
            id, title, description, status, category, address,
            ST_AsGeoJSON(geometry) as geometry_json,
            author_name, author_role
        FROM initiatives
        WHERE 1=1
    """
    params = {}
    
    if status:
        query += " AND status = :status"
        params["status"] = status
    
    query += " ORDER BY created_at DESC"
    
    result = db.execute(text(query), params)
    rows = result.fetchall()
    
    features = []
    for row in rows:
        import json
        geom = json.loads(row.geometry_json)
        features.append({
            "type": "Feature",
            "geometry": geom,
            "properties": {
                "id": str(row.id),
                "title": row.title,
                "status": row.status,
                "category": row.category or "",
                "address": row.address or ""
            }
        })
    
    return {
        "type": "FeatureCollection",
        "features": features
    }
