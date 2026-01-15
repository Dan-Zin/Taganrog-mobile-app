# Быстрый старт

## Шаг 1: Подготовка тайлов

Поместите файл `taganrog.mbtiles` в папку `tiles/`:

```bash
mkdir -p tiles
# Скопируйте ваш файл taganrog.mbtiles в папку tiles/
```

## Шаг 2: Запуск серверов

```bash
# Запуск всех сервисов
docker-compose up -d

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f
```

## Шаг 3: Проверка работы

Откройте в браузере:
- API: http://localhost/api/health
- API документация: http://localhost/api/docs
- Tile Server: http://localhost:8000

## Шаг 4: Настройка мобильного приложения

1. Откройте `app/src/main/java/com/example/taganrog_map/data/Config.kt`
2. Замените IP адрес на IP вашего компьютера в локальной сети:
   ```kotlin
   const val API_BASE_URL = "http://192.168.X.X:80/api"
   const val TILE_SERVER_URL = "http://192.168.X.X:8000"
   ```
3. Соберите и запустите приложение в Android Studio

## Шаг 5: Проверка базы данных

```bash
# Подключение к базе данных
docker-compose exec db psql -U taganrog_user -d taganrog_db

# Проверка инициатив
SELECT id, title, status FROM initiatives;
```

## Остановка

```bash
docker-compose down
```

## Удаление данных (осторожно!)

```bash
docker-compose down -v
```
