-- Включение PostGIS
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Таблица инициатив
CREATE TABLE IF NOT EXISTS initiatives (
    id SERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RED', 'YELLOW', 'GREEN')),
    category VARCHAR(100),
    address VARCHAR(500),
    geometry GEOMETRY(POINT, 4326) NOT NULL,
    author_id VARCHAR(100),
    author_name VARCHAR(200),
    author_role VARCHAR(50) DEFAULT 'Гражданин',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индекс для пространственных запросов
CREATE INDEX IF NOT EXISTS idx_initiatives_geometry ON initiatives USING GIST (geometry);
CREATE INDEX IF NOT EXISTS idx_initiatives_status ON initiatives (status);
CREATE INDEX IF NOT EXISTS idx_initiatives_created_at ON initiatives (created_at DESC);

-- Таблица медиа вложений инициатив
CREATE TABLE IF NOT EXISTS initiative_media (
    id SERIAL PRIMARY KEY,
    initiative_id INT NOT NULL REFERENCES initiatives(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    media_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_initiative_media_initiative_id ON initiative_media (initiative_id);

-- Таблица улиц (пример структуры для определения адреса)
CREATE TABLE IF NOT EXISTS streets (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    geometry GEOMETRY(LINESTRING, 4326) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_streets_geometry ON streets USING GIST (geometry);
CREATE INDEX IF NOT EXISTS idx_streets_name ON streets (name);

-- Таблица районов
CREATE TABLE IF NOT EXISTS districts (
    id SERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    geometry GEOMETRY(POLYGON, 4326) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_districts_geometry ON districts USING GIST (geometry);

-- Функция определения улицы по координатам
CREATE OR REPLACE FUNCTION get_street_name(lat DOUBLE PRECISION, lon DOUBLE PRECISION)
RETURNS VARCHAR(200) AS $$
DECLARE
    street_name VARCHAR(200);
BEGIN
    SELECT name INTO street_name
    FROM streets
    WHERE ST_DWithin(
        geometry::geography,
        ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography,
        50  -- 50 метров
    )
    ORDER BY ST_Distance(
        geometry::geography,
        ST_SetSRID(ST_MakePoint(lon, lat), 4326)::geography
    )
    LIMIT 1;
    
    RETURN COALESCE(street_name, 'Неизвестная улица');
END;
$$ LANGUAGE plpgsql;

-- Функция определения района по координатам
CREATE OR REPLACE FUNCTION get_district_name(lat DOUBLE PRECISION, lon DOUBLE PRECISION)
RETURNS VARCHAR(200) AS $$
DECLARE
    district_name VARCHAR(200);
BEGIN
    SELECT name INTO district_name
    FROM districts
    WHERE ST_Contains(
        geometry,
        ST_SetSRID(ST_MakePoint(lon, lat), 4326)
    )
    LIMIT 1;
    
    RETURN COALESCE(district_name, 'Неизвестный район');
END;
$$ LANGUAGE plpgsql;

-- Триггер для автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_initiatives_updated_at
    BEFORE UPDATE ON initiatives
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Вставка тестовых данных улиц (пример для Таганрога)
-- В реальном проекте эти данные должны загружаться из OpenStreetMap или муниципальных источников
INSERT INTO streets (name, geometry) VALUES
    ('ул. Петровская', ST_SetSRID(ST_MakeLine(
        ST_MakePoint(38.895, 47.235),
        ST_MakePoint(38.900, 47.240)
    ), 4326))
ON CONFLICT DO NOTHING;

INSERT INTO streets (name, geometry) VALUES
    ('ул. Греческая', ST_SetSRID(ST_MakeLine(
        ST_MakePoint(38.897, 47.237),
        ST_MakePoint(38.902, 47.242)
    ), 4326))
ON CONFLICT DO NOTHING;

-- Вставка тестовых инициатив
INSERT INTO initiatives (title, description, status, category, address, geometry, author_name) VALUES
    ('Яма во дворе дома №15 по ул. Петровская', 
     'Большая яма на проезжей части создает опасность для водителей и пешеходов. Необходим срочный ремонт.',
     'RED', 'Дороги', 'ул. Петровская, 15',
     ST_SetSRID(ST_MakePoint(38.897, 47.236), 4326),
     'Иван Петров')
ON CONFLICT DO NOTHING;

INSERT INTO initiatives (title, description, status, category, address, geometry, author_name) VALUES
    ('Недостаточное освещение на ул. Греческой',
     'В вечернее время на улице очень темно. Нужно установить дополнительные фонари.',
     'YELLOW', 'Освещение', 'ул. Греческая',
     ST_SetSRID(ST_MakePoint(38.899, 47.237), 4326),
     'Мария Сидорова')
ON CONFLICT DO NOTHING;

INSERT INTO initiatives (title, description, status, category, address, geometry, author_name) VALUES
    ('Новая детская площадка в парке',
     'Отлично выполненная работа! Дети в восторге от новой площадки.',
     'GREEN', 'Инфраструктура', 'Центральный парк',
     ST_SetSRID(ST_MakePoint(38.895, 47.238), 4326),
     'Алексей Козлов')
ON CONFLICT DO NOTHING;
