package com.example.taganrog_map.data

object MockInitiatives {
    val items = listOf(
        Initiative(
            id = "1",
            lat = 47.2360,
            lon = 38.8970,
            title = "Яма во дворе дома №15 по ул. Петровская",
            status = Status.RED,
            description = "Большая яма на проезжей части создает опасность для водителей и пешеходов. Необходим срочный ремонт.",
            address = "ул. Петровская, 15",
            category = "Дороги",
            author = Author("1", "Иван Петров", "Гражданин"),
            createdAt = "15.03.2024"
        ),
        Initiative(
            id = "2",
            lat = 47.2372,
            lon = 38.8991,
            title = "Недостаточное освещение на ул. Греческой",
            status = Status.YELLOW,
            description = "В вечернее время на улице очень темно. Нужно установить дополнительные фонари.",
            address = "ул. Греческая",
            category = "Освещение",
            author = Author("2", "Мария Сидорова", "Гражданин"),
            createdAt = "10.03.2024"
        ),
        Initiative(
            id = "3",
            lat = 47.2385,
            lon = 38.8954,
            title = "Новая детская площадка в парке",
            status = Status.GREEN,
            description = "Отлично выполненная работа! Дети в восторге от новой площадки.",
            address = "Центральный парк",
            category = "Инфраструктура",
            author = Author("3", "Алексей Козлов", "Гражданин"),
            createdAt = "01.02.2024"
        ),
        Initiative(
            id = "4",
            lat = 47.2379,
            lon = 38.8968,
            title = "Стихийная свалка мусора на пустыре",
            status = Status.RED,
            description = "Незаконная свалка бытового мусора растет с каждым днем. Требуется уборка и установка заграждения.",
            address = "пер. Некрасовский",
            category = "Экология",
            author = Author("4", "Елена Волкова", "Гражданин"),
            createdAt = "20.03.2024"
        ),
        Initiative(
            id = "5",
            lat = 47.2350,
            lon = 38.8980,
            title = "Ремонт тротуара на набережной",
            status = Status.YELLOW,
            description = "Работы по ремонту тротуара начались. Ожидается завершение в течение месяца.",
            address = "Набережная",
            category = "Дороги",
            author = Author("5", "Петр Новиков", "Администрация"),
            createdAt = "05.03.2024"
        ),
        Initiative(
            id = "6",
            lat = 47.2390,
            lon = 38.8940,
            title = "Озеленение сквера у театра",
            status = Status.YELLOW,
            description = "Предложение по высадке новых деревьев и кустарников в сквере для улучшения экологии.",
            address = "пл. Театральная",
            category = "Экология",
            author = Author("6", "Ольга Смирнова", "Гражданин"),
            createdAt = "25.03.2024"
        )
    )
}
