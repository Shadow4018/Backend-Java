# TMS — Translation Management System

Лаб.№1 «Spring Security» семестрового проєкту. Предметна область: система
управління перекладами (проєкти → текстові ключі → варіанти перекладу),
ролі ADMIN / TRANSLATOR / EDITOR.

## Стек
Spring Boot 3.5, Java 21, Spring Data JPA + Hibernate, MapStruct, Liquibase,
MariaDB, Spring Security (OIDC через Keycloak + JWT resource server).

## Запуск

1. Потрібні JDK 21, Maven, Docker.
2. Підняти Keycloak і MariaDB:
   ```bash
   docker compose up -d
   ```
   Realm `course-realm` імпортується автоматично з `keycloak/course-realm.json`.
   Консоль адміністратора Keycloak: http://localhost:8080 (admin/admin).

   Тестові користувачі:
   | Логін        | Пароль          | Ролі               |
   |--------------|-----------------|--------------------|
   | belialu      | belialu123      | USER, ADMIN        |
   | translator   | translator123   | USER, TRANSLATOR   |
   | editor       | editor123       | USER, EDITOR       |
   | bob          | bob123          | USER               |

3. Запустити застосунок:
   ```bash
   mvn spring-boot:run
   ```
   Liquibase створить схему й тестові дані (контекст `dev`, автоматично
   активний через `spring.liquibase.contexts` у `application.yml`).

4. UI: http://localhost:8081/tsm.html — вхід через кнопку «Увійти (Keycloak)».
   Спочатку відкривається каталог проєктів; кнопка «Відкрити каталог» на
   проєкті веде в робочий простір із трьома вкладками: Оригінали, Переклади,
   Глосарій (Оригінали — за замовчуванням). Кнопка «Переклади →» на
   оригінальному рядку перемикає на вкладку Переклади й одразу фільтрує на
   цей ключ.
5. Postman: імпортувати `postman/TMS.postman_collection.json`, спершу
   виконати запити з теки «0. Auth», щоб отримати JWT для ADMIN/TRANSLATOR/EDITOR.

## Автентифікація
- **OIDC** — для UI (`/`, `/tsm.html`): вхід через `/oauth2/authorization/keycloak`.
  UI обмінює сесію на JWT через `/ui/token` і далі ходить у `/api/**` з `Authorization: Bearer`.
- **JWT** — для прямих API-клієнтів (curl/Postman): `POST /realms/course-realm/protocol/openid-connect/token`.

## Обмеження доступу (два незалежні способи)
- `SecurityFilterChain` (`SecurityConfig.apiSecurityFilterChain`): напр. створення/зміна/
  видалення проєкту — лише `ADMIN`.
- `@PreAuthorize` (`TranslationService.approve`): затвердження перекладу — лише
  `ADMIN`/`EDITOR`, незалежно від правил filter chain.

## Нумерація в UI
Стовпець `#` у списках текстових ключів і перекладів — це порядкова позиція
в межах проєкту/ключа (1, 2, 3, ...), а не реальний первинний ключ у базі.
Справжній `id` — наскрізний для всієї таблиці й ніколи не скидається (це
навмисно: PK не повинен повторюватись чи переприсвоюватись); саме тому
перший текстовий ключ у другому проєкті показує `#1`, хоча його `id` в базі
може бути, наприклад, `7`.

## Права перекладачів на власні пропозиції
- `TRANSLATOR` бачить кнопку «Редагувати» лише на своїх ще не затверджених
  пропозиціях (замість «Затвердити», якої в нього й так немає); може їх
  видалити. Чужі пропозиції — лише перегляд.
- `ADMIN`/`EDITOR` бачать «Затвердити» на чужих pending-пропозиціях і можуть
  видалити будь-яку.
- Перевірка власності (`translatorName` == ім'я користувача з токена) — у
  `TranslationService.update`/`delete`, а не лише в UI: прямий виклик API під
  чужим токеном отримає 403.
- Затверджену (`APPROVED`) пропозицію редагувати не можна — тільки видалити
  й запропонувати нову.

## Тести
```bash
mvn test
```
