# `telegram-client`

Библиотека для Telegram-ботов на базе `kotlin-telegram-bot`, которая использует API проекта `ksolow-tools`.
Зависимость на `kotlin-telegram-bot` экспортируется транзитивно, поэтому в приложении-потребителе ее отдельно подключать не нужно.

Что есть в модуле:

- экстеншены `sendMessageWithChunking` и `sendPhotoWithTruncatedCaption`
- DSL для команд через интерфейс `Command`
- command handlers:
  `handleWeather`, `handleHolidays`, `handleDay`, `handleStyle`, `handleExplain`, `handleTranslate`, `handleToday`, `handleTomorrow`, `handleCat`
- scheduler helper: `KsolowToolsTelegram.scheduleMessageSupport`
- text handlers:
  `cacheMessageForDay`, `handleDirectAddress`
- проверка доступа через `forAllowedChats`
- хранение выбранного стиля чата в MongoDB
- хранение сообщений за день в MongoDB с `dayKey`
- шифрование сообщений в MongoDB через AES/GCM

## Зависимости

Модуль рассчитан на использование вместе с backend-модулем этого репозитория.

Нужные backend endpoints:

- `GET /tools/styles`
- `GET /tools/cat`
- `GET /weather/current`
- `GET /weather/current/styled`
- `GET /day/today`
- `GET /day/tomorrow`
- `GET /day/holidays/today`
- `GET /day/holidays/today/styled`
- `POST /day/summary/styled`
- `POST /day/morning-message/styled`
- `POST /day/evening-message/styled`
- `POST /ai/proxy/request/styled`
- `POST /ai/proxy/explain/styled`
- `POST /ai/proxy/translate/styled`

## Конфигурация

Перед использованием модуль нужно один раз инициализировать:

```kotlin
KsolowToolsTelegram.configure(
    KsolowToolsTelegramClientConfig(
        serviceUrl = "http://localhost:8080",
        mongoUrl = "mongodb://localhost:27017",
        mongoDatabase = "example-bot",
        messagesEncryptionKey = "base64-or-raw-key",
        dayZoneId = "Europe/Moscow",
        allowedIds = setOf(123L, 456L),
        defaultStyle = "swear"
    )
)
```

Либо можно собрать всё в одном месте через DSL и сразу получить `Bot`:

```kotlin
import ru.ksolowtools.telegram.client.*

@Bean
fun telegramBot() = ksolowToolsTelegramBot {
    serviceUrl = "http://localhost:8080"
    mongoUrl = "mongodb://localhost:27017"
    mongoDatabase = "example-bot"
    messagesEncryptionKey = "base64-or-raw-key"
    dayZoneId = "Europe/Moscow"
    allowedIds = setOf(123L, 456L)
    defaultStyle = "swear"

    bot {
        token = "telegram-token"
        dispatch {
            command(BotCommand.WEATHER) {
                forAllowedChats {
                    handleWeather()
                }
            }

            message {
                forAllowedChats {
                    cacheMessageForDay()
                    handleDirectAddress(botUsername = "my_bot")
                }
            }
        }
    }
}
```

Для этого DSL модуль также реэкспортирует удобные entrypoint'ы `bot`, `dispatch` и `message` из своего пакета.

### Поля `KsolowToolsTelegramClientConfig`

- `serviceUrl`: base URL backend-сервиса
- `mongoUrl`: адрес MongoDB
- `mongoDatabase`: имя базы данных
- `messagesEncryptionKey`: ключ шифрования сообщений за день
- `dayZoneId`: таймзона для `dayKey`
- `allowedIds`: список разрешенных chat id; если пустой, фильтра нет
- `defaultStyle`: стиль по умолчанию, если для чата стиль еще не сохранен
- `weatherLocationAliases`: алиасы городов для `/weather`
  По умолчанию есть алиасы для `spb` и `krasnoyarsk`.

## Команды

Можно использовать свой enum с интерфейсом `Command`:

```kotlin
enum class BotCommand(override val value: String) : Command {
    WEATHER("weather"),
    HOLIDAYS("holidays"),
    DAY("day"),
    STYLE("style"),
    EXPLAIN("explain"),
    TRANSLATE("translate"),
    TODAY("today"),
    TOMORROW("tomorrow"),
    CAT("cat")
}
```

После этого доступен extension для `Dispatcher`:

```kotlin
command(BotCommand.WEATHER) {
    handleWeather()
}
```

## Пример интеграции

```kotlin
import ru.ksolowtools.telegram.client.*

enum class BotCommand(override val value: String) : Command {
    WEATHER("weather"),
    HOLIDAYS("holidays"),
    DAY("day"),
    STYLE("style"),
    EXPLAIN("explain"),
    TRANSLATE("translate"),
    TODAY("today"),
    TOMORROW("tomorrow"),
    CAT("cat")
}

fun createBot(token: String, username: String) = bot {
    this.token = token

    dispatch {
        command(BotCommand.WEATHER) {
            forAllowedChats {
                handleWeather()
            }
        }

        command(BotCommand.HOLIDAYS) {
            forAllowedChats {
                handleHolidays()
            }
        }

        command(BotCommand.DAY) {
            forAllowedChats {
                handleDay()
            }
        }

        command(BotCommand.STYLE) {
            forAllowedChats {
                handleStyle()
            }
        }

        command(BotCommand.EXPLAIN) {
            forAllowedChats {
                handleExplain()
            }
        }

        command(BotCommand.TRANSLATE) {
            forAllowedChats {
                handleTranslate()
            }
        }

        command(BotCommand.TODAY) {
            forAllowedChats {
                handleToday()
            }
        }

        command(BotCommand.TOMORROW) {
            forAllowedChats {
                handleTomorrow()
            }
        }

        command(BotCommand.CAT) {
            forAllowedChats {
                handleCat()
            }
        }

        message {
            forAllowedChats {
                cacheMessageForDay()
                handleDirectAddress(botUsername = username)
            }
        }
    }
}
```

## Что хранится в MongoDB

Коллекция `chats`:

- `chatId`
- `name`
- `style`

Коллекция `dayMessages`:

- `chatId`
- `dayKey`
- `message`

`message` хранится в зашифрованном виде.

## Замечания

- `/day` использует только сообщения за текущий `dayKey`
- `dayKey` вычисляется в таймзоне `dayZoneId`
- если backend недоступен, часть handlers использует fallback-ответы
- список доступных стилей берется из backend и кэшируется в клиенте

## Scheduler helper

Для morning/evening flow можно использовать:

```kotlin
val morningText = KsolowToolsTelegram.scheduleMessageSupport.morningMessage(chatId)

val evening = KsolowToolsTelegram.scheduleMessageSupport.eveningMessage(chatId)
val text = evening.text
val imageUrl = evening.imageUrl
```

`eveningMessage(chatId)` сам резолвит стиль чата, достает сообщения за текущий `dayKey` и вызывает backend.

## Публикация в GitHub Packages

В модуле уже настроен `maven-publish`.

Для публикации нужны:

- GitHub repository owner
- GitHub repository name
- GitHub token с правом публикации пакетов

Пример локальной публикации:

```bash
export GITHUB_ACTOR=<github-user>
export GITHUB_TOKEN=<github-token>

./gradlew :telegram-client:publish \
  -PgithubOwner=koctuk999 \
  -PgithubRepository=ksolow-tools
```

Можно также передавать owner/repo через `GITHUB_REPOSITORY=<owner>/<repo>`.

Для текущего репозитория это:

```bash
export GITHUB_REPOSITORY=koctuk999/ksolow-tools
```

Артефакт публикуется с координатами:

```text
ru.ksolowtools:telegram-client:<version>
```

### Подключение в другом проекте

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/koctuk999/ksolow-tools")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("ru.ksolowtools:telegram-client:0.0.3-SNAPSHOT")
}
```
