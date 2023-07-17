# Раздел 1: Введение
## 1.1 Описание проекта
TelegramNotes - это телеграм-бот, разработанный для удобного сохранения, редактирования и удаления заметок. Бот предоставляет пользователю возможность хранить важную информацию и напоминания в виде текстовых заметок, которые могут быть доступны в любое время через мессенджер Telegram.

## 1.2 Цель и целевая аудитория
Цель TelegramNotes состоит в предоставлении простого и удобного способа сохранения и организации заметок с использованием Telegram. Бот может быть полезен для широкого круга пользователей, включая студентов, профессионалов, предпринимателей и всех, кто нуждается в системе управления заметками.

## 1.3 Возможности проекта
* Создание новых заметок: пользователь может создавать новые заметки, указывая заголовок и содержимое.
* Редактирование заметок: пользователь может вносить изменения в существующие заметки, включая заголовок и содержимое.
* Удаление заметок: пользователь может удалять не нужные заметки из своего списка.
* Напоминания (планируемая функция): планируется добавление функциональности напоминаний, чтобы пользователь мог установить определенное время или дату для получения уведомления о заметке.

## 1.4 Технологии и инструменты
TelegramNotes разработан с использованием следующих технологий и инструментов:

* Язык программирования: **Java**.
* Фреймворк для разработки приложений на платформе Java: **Spring Framework**.
* Библиотека для разработки телеграм-ботов: **telegrambots**.
* Хранение данных: база данных **MySQL** и **Spring Data JPA**.
* Система контроля версий: **Git**.
* Инструмент для управления проектами и автоматизации сборки: **Maven**.
* Интеграция с Telegram API для обработки команд и взаимодействия с пользователями.

________________________________________________________________________________________________

# Раздел 2: Установка и настройка
## 2.1 Требования к системе
Для установки и запуска TelegramNotes вам потребуется следующее:
* Java Development Kit (JDK) версии 8 или выше
* Учетная запись Telegram
* Доступ к интернету
## 2.2 Установка
Убедитесь, что JDK установлен на вашей системе. Если JDK не установлен, скачайте и установите JDK для вашей операционной системы, следуя инструкциям,
доступным на официальном веб-сайте Oracle или OpenJDK. Убедитесь, что у вас установлен Maven. Если Maven не установлен, скачайте и установите Maven, 
следуя инструкциям, доступным на официальном веб-сайте Apache Maven.

1. Скачайте архив проекта TelegramNotes с GitHub и распакуйте его в выбранную вами директорию.
2. Перейдите в каталог проекта TelegramNotes:

``cd TelegramNotes``

## 2.3 Настройка
1. Создайте бота в Telegram через @BotFather и получите токен доступа.
2. Откройте файл *src/main/resources/application.properties* в каталоге проекта TelegramNotes и добавьте следующую информацию:

``bot.token=YOUR_TELEGRAM_API_TOKEN``

3. Замените ``YOUR_TELEGRAM_API_TOKEN`` на полученный токен доступа от BotFather.
4. Сохраните файл application.properties.
5. Запуск с помощью Maven
6. Откройте командную строку и перейдите в каталог проекта TelegramNotes. Соберите проект и соберите исполняемый JAR-файл, выполнив следующую команду:

``mvn clean install``

7. Запустите проект, выполнив следующую команду:

``java -jar target/TelegramNotes.jar``

**Запуск с помощью IntelliJ IDEA**
1. Откройте IntelliJ IDEA и выберите "Open" из главного меню.
2. Перейдите в каталог проекта TelegramNotes и выберите файл pom.xml для открытия проекта.
3. В IntelliJ IDEA откройте файл src/main/java/com/example/telegramnotes/TelegramNotesApplication.java.
4. Щелкните правой кнопкой мыши на классе TelegramNotesApplication и выберите "Run TelegramNotesApplication".
5. Найдите своего бота в Telegram по его имени и начните диалог.

Бот будет готов к использованию. Он автоматически создаст базу данных для хранения заметок.

________________________________________________________________________________________________________________

# Раздел 3: Руководство пользователя
В этом разделе представлено руководство пользователя для работы с ботом TelegramNotes. Описаны основные функции и инструкции по использованию.

## 3.1 Начало работы
Найдите бота TelegramNotes в Telegram по его имени @NoteKeep_Bot или перейдите по ссылке https://t.me/NoteKeep_Bot.

Начните диалог с ботом, нажав на кнопку "Start" или отправив команду /start.



Бот будет предоставлять вам инструкции и доступ к функциям через интерфейс Telegram.

## 3.2 Создание новой заметки
Для создания новой заметки используйте встроенную клавиатуру введите команду "Create note ✏️".

![photo_2023-07-17_15-48-20](https://github.com/roman50021/TelegramNotes/assets/103030747/0be37e0b-c8fd-4c1e-929c-58c196fd029a)

Следуйте инструкциям бота и введите содержимое заметки. Первые 15 символов будут сохранены как заголовок.

После ввода информации заметка будет сохранена, и вы получите подтверждение. 

![photo_2023-07-17_15-44-50](https://github.com/roman50021/TelegramNotes/assets/103030747/6955c1bb-a69b-4c43-904e-a936c24fc09d)

Получить список из ваших заметок и получить полный текст вы можете командой "My notes 📁". 
Список заметок будет отображено в виде клавиатуры при нажатии на кнопку Вы можете получить полный текст заметки.
Вернуться на главное меню команда "Back ↩️"

![photo_2023-07-17_15-44-50 (2)](https://github.com/roman50021/TelegramNotes/assets/103030747/fcccb6f4-2c1e-44c5-be09-cc2e74a1638e)

## 3.3 Редактирование заметок
Для просмотра списка всех сохраненных заметок и их редактирование нажмите кнопку или введите команду "Edit notes 📝".

Бот покажет вам список заметок с их заголовками. Заголовки будут обращаться как кнопке соответствующих заметок. 
После нажатия кнопки заметки вы сможете редактировать заметку и получите её прошлое содержание. 

Следуйте инструкциям бота для внесения изменений в заголовок или содержимое заметки.

3.4 Удаление заметок
Для удаления заметки введите команду /delete и укажите идентификатор заметки.

Бот запросит подтверждение удаления. Подтвердите, нажав на кнопку "Да" или отправив команду /yes.

Заметка будет удалена из списка.

3.5 Напоминания (планируемая функция)
Планируется добавление функциональности напоминаний, чтобы установить определенное время или дату для получения уведомления о заметке.

Когда эта функция будет доступна, вы получите инструкции от бота о том, как установить напоминание для заметки.
























