# Infrastruktura backendu

Dokumentacja techniczna. Opisuje mechanizmy, na których stoją funkcje biznesowe: jak praca jest
odkładana na później, co się dzieje, gdy się nie uda, jak wychodzą maile i jak działa logowanie.

Wszystkie godziny w systemie są liczone z wstrzykiwanego `Clock` (czas warszawski), nigdy z
`LocalDateTime.now()` bez argumentu.

---

## Mapa

| mechanizm | po co | gdzie | tabela |
|---|---|---|---|
| EventBus | reakcja na fakt w tle, bez czekania requestu | `com.app.prod.eventbus` | — |
| AsyncJobRunner | job w tle z retry w pamięci | `com.app.prod.job.async` | `job_error` |
| Scheduled jobs | trwała kolejka zadań na konkretną godzinę | `com.app.prod.job.scheduled` | `scheduled_job` |
| Job error | dziennik jobów, które ostatecznie padły, + ręczny rerun | `com.app.prod.job` | `job_error` |
| Email delivery | wysyłka maili z zapisem, kto co dostał | `com.app.prod.mail` | `email_delivery` |
| Activation token | jednorazowy link aktywujący konto | `com.app.prod.authorization` | `activation_token` |
| Refresh token | sesja logowania z rotacją | `com.app.prod.config.security` | `refresh_token` |

Najważniejsze rozróżnienie:

- **Event** to fakt, który *już się stał*. Nie jest zapisywany, nie jest ponawiany — wykonuje się raz,
  „best effort".
- **Job** to praca do wykonania. Jest danymi (record serializowany do JSON), więc da się go zapisać
  w bazie, ponowić i uruchomić jeszcze raz ręcznie.

Jeżeli coś *musi* się wydarzyć (mail o potwierdzeniu wydarzenia, zamknięcie głosowania o
deadline), to jest to job w `scheduled_job`, nie event.

---

## EventBus

Własna, mała szyna zdarzeń wewnątrz procesu.

```
serwis ──publish(event)──▶ EventBus ──(po commicie)──▶ EventDispatcher ──▶ VirtualThreadEventExecutor ──▶ EventHandler.handle()
```

### Elementy

- `AppEvent` — interfejs-znacznik. Implementacje to recordy z identyfikatorami i prostymi wartościami.
  Nigdy rekordy jOOQ i nigdy `BuildingScope` — handler wykonuje się długo po autoryzacji requestu.
- `EventHandler<E>` — `handle(E event)` + `eventType()`. Każdy handler to bean Springa.
- `EventBus` — przy starcie zbiera wszystkie handlery i indeksuje je po typie eventu.
  **Jeden typ eventu = dokładnie jeden handler.** Dwa handlery dla tego samego typu wywracają start
  aplikacji (`EVENT_HANDLER_DUPLICATED`); publikacja eventu bez handlera rzuca
  `EVENT_HANDLER_NOT_FOUND` od razu, w wątku requestu.
- `EventDispatcher` — nadaje eventowi id (do logów), wykonuje handler w executorze, łapie każdy wyjątek
  i tylko go loguje (`failed and will not be retried`).
- `VirtualThreadEventExecutor` — jeden wirtualny wątek na event. Przy zamknięciu aplikacji czeka do
  30 s na eventy w locie, potem je porzuca.

### Transakcje

Jeżeli `publish` jest wołany wewnątrz transakcji, event **czeka na commit**
(`TransactionSynchronization.afterCommit`). Rollback = event nigdy nie poleci. Dzięki temu handler
zawsze widzi dane, które spowodowały event. Poza transakcją event idzie od razu.

### Gdzie jest używany

| event | handler | kto publikuje |
|---|---|---|
| `AccountsInvitedEvent` | `AccountInvitationsMailHandler` — wysyła maile z zaproszeniem | `PendingAccountService.createPendingAccounts` |

### Kiedy NIE używać

Na Cloud Run CPU jest dławione poza obsługą requestu. Praca zlecona na wątek w tle *po* wysłaniu
odpowiedzi potrafi stanąć na długo. Do wszystkiego, co musi się wykonać, używaj `scheduled_job`.
EventBus nadaje się do rzeczy, których utrata jest akceptowalna albo naprawialna przez użytkownika
(zaproszenie można wysłać ponownie — patrz [Email delivery](#email-delivery)).

### Dodanie nowego eventu

1. Record implementujący `AppEvent` w pakiecie funkcji (np. `user/events`).
2. Bean implementujący `EventHandler<TwójEvent>`, `eventType()` zwraca klasę eventu.
3. `eventBus.publish(...)` w serwisie — najlepiej w transakcji, żeby event czekał na commit.

---

## Joby

Job to record implementujący `Job` (`com.app.prod.job.jobs`):

```java
public record EventMailJob(EmailDeliveryType type, UUID eventId, UUID slotId, LocalDateTime groupConfirmBy) implements Job {
    @Override
    public void run(JobRegistry jobRegistry) {
        jobRegistry.sendEventMail(this);
    }
}
```

- Job jest czystymi danymi — serializuje się do JSONB z polem `@type`. Każdy nowy job **musi** być
  dopisany do `@JsonSubTypes` w `Job`, inaczej nie da się go odczytać z bazy.
- Job nie trzyma beanów. Dostaje `JobRegistry` od tego, kto go uruchamia, i przez niego woła serwisy.
  `JobRegistry` to jedyne miejsce, gdzie joby dotykają logiki biznesowej.

| job | co robi | uruchamiany przez |
|---|---|---|
| `SyncUserJob` | synchronizuje użytkownika z serwisem czatu | `AsyncJobRunner` |
| `ConversationsCreateJob` | zakłada konwersacje w serwisie czatu | `AsyncJobRunner` |
| `EventDeadlineJob` | stosuje deadline'y wydarzenia (głosowanie, okno potwierdzenia) | `scheduled_job` |
| `EventMailJob` | wysyła mail o zmianie terminu wydarzenia | `scheduled_job` |
| `NoOpJob` | nic — do testowania kolejki | dowolny |

Są dwie drogi uruchomienia joba.

### AsyncJobRunner (starsza droga, w pamięci)

`asyncJobRunner.execute(job)` — job leci na pulę wątków `taskExecutor` (4–10 wątków, kolejka 50).

- `@Retryable`: 3 próby, backoff 2 s → 4 s.
- Po trzeciej porażce `@Recover` zapisuje job do `job_error` ze statusem `FAILED`.
- Nic nie jest zapisywane *przed* wykonaniem — restart instancji w trakcie gubi job bez śladu.
- Te same ograniczenia Cloud Run co EventBus.

Używany tylko przez integrację z serwisem czatu (`UserService`, `BuildingService`). Nowych rzeczy tą
drogą nie dodajemy.

### Scheduled jobs (trwała kolejka)

`scheduledJobService.schedule(job, runAt, dedupeKey)` zapisuje job w `scheduled_job`. Wykona się
najwcześniej o `runAt` — `now` oznacza „jak najszybciej, ale po commicie".

```
schedule() ──▶ scheduled_job (PENDING)
                      │
   drain: claim ──────┘──▶ RUNNING (locked_until = now + 5 min, attempts + 1)
                      │
          job.run() ──┼── ok ─────▶ SUCCESS
                      └── wyjątek ─▶ attempts < 3 ? PENDING (run_at = now + 5 min × attempts)
                                                  : FAILED + wpis w job_error
```

#### Planowanie i deduplikacja

- `dedupeKey` mówi, *czego dotyczy* job, np. `event_voting_deadline:<eventId>`. `schedule` najpierw
  anuluje (`CANCELLED`) wszystko otwarte (`PENDING`/`RUNNING`) pod tym samym kluczem, więc ponowne
  zaplanowanie **zastępuje** poprzednie. Unikalny indeks częściowy pilnuje, żeby pod jednym kluczem
  był co najwyżej jeden otwarty job.
- `dedupeKey = null` — dla pracy, która może się legalnie kolejkować wiele razy (np. maile o
  wydarzeniu).
- `scheduledJobService.cancel(dedupeKey)` — odwołuje job, np. gdy okno potwierdzenia przestaje
  obowiązywać.
- `schedule` jest `@Transactional` — wołany z transakcji serwisu, job pojawia się w kolejce dopiero
  po jej commicie i znika przy rollbacku. To jest sposób na „zrób X po zapisaniu zmiany".

#### Drain — kto wykonuje kolejkę

`ScheduledJobDrainer.drainDue(now)`:

1. **Claim** — jedno zapytanie `UPDATE ... WHERE id IN (SELECT ... FOR UPDATE SKIP LOCKED LIMIT 50)`
   w osobnej, od razu commitowanej transakcji. Bierze joby `PENDING` z `run_at <= now` oraz
   `RUNNING` z wygasłym leasem. `SKIP LOCKED` sprawia, że dwa równoległe drainy nigdy nie dostaną
   tego samego wiersza.
2. Każdy job wykonuje się **bez otaczającej transakcji**. Job sam otwiera to, czego potrzebuje. Mail,
   który już wyszedł, nie może zostać wycofany razem z porażką przy następnym.
3. Wynik zapisywany osobną transakcją (`markSuccess` / `retryAt` / `markFailed`).
4. Payload, którego nie da się odczytać (np. job usunięty z `@JsonSubTypes`), od razu kończy jako
   `FAILED` bez ponawiania.

**Lease (5 min)** chroni przed padnięciem instancji w trakcie: wiersz zostaje `RUNNING`, po 5
minutach następny drain go przejmie (jeśli `attempts < 3`). Job trwający dłużej niż lease może
zostać wykonany podwójnie — joby muszą być idempotentne.

Drain wołają:

| środowisko | kto | jak |
|---|---|---|
| prod (Cloud Run) | Cloud Scheduler, cron | `POST /v1/api/internal/jobs/drain` z nagłówkiem `X-Job-Token` |
| dev | `ScheduledJobPoller` (`@Scheduled`) | co `jobs.poller.interval-ms` (30 s), włączany `jobs.poller.enabled` |

Endpoint `/internal/**` nie przyjmuje JWT. `JobTokenAuthFilter` porównuje `X-Job-Token` z
`jobs.drain-token` (stałoczasowo) i nadaje rolę `INTERNAL`. Pusty `jobs.drain-token` = endpoint
zamknięty dla wszystkich. Odpowiedź: `{claimed, succeeded, failed}`. Ręczne wywołanie to sposób na
popchnięcie zablokowanej kolejki.

#### Idempotentność — wymóg dla każdego scheduled joba

Ten sam job może wykonać się więcej niż raz (retry, wygasły lease). Przykłady, jak to jest rozwiązane:

- `EventMailJob` → `MailDeliveryService.send` pomija odbiorców, którzy już dostali mail.
- `EventDeadlineJob` → `applyPassedDeadlines` sprawdza aktualny stan wydarzenia, a nie zakłada go.
- Job, którego obiekt zniknął (usunięte wydarzenie), kończy się sukcesem bez działania, zamiast
  failować trzy razy.

#### Dodanie nowego scheduled joba

1. Record implementujący `Job` w `job/jobs`, wpis w `@JsonSubTypes`.
2. Metoda w `JobRegistry` delegująca do serwisu.
3. `scheduledJobService.schedule(...)` z serwisu, w jego transakcji. Klucz deduplikacji, jeżeli
   job dotyczy „jednej rzeczy w danym czasie".
4. Upewnij się, że ponowne wykonanie niczego nie psuje.

---

## Job error

`job_error` to dziennik jobów, które ostatecznie się nie udały — z obu dróg:

- z `AsyncJobRunner` po wyczerpaniu `@Retryable`,
- z `scheduled_job` po trzeciej nieudanej próbie (wiersz w `scheduled_job` dostaje `FAILED`, a w
  `job_error` pojawia się kopia payloadu i komunikat błędu).

| kolumna | znaczenie |
|---|---|
| `job_name` | prosta nazwa klasy joba |
| `payload` | job w JSONB — z niego robi się rerun |
| `status` | `FAILED` po zapisie, potem `SUCCESS`/`FAILED` po rerunie |
| `error_message` | komunikat wyjątku z ostatniej próby |
| `rerun_at` | kiedy ostatnio ponawiano |

### Rerun (tylko ADMIN)

| endpoint | co robi |
|---|---|
| `GET /job?status=FAILED` | lista wpisów |
| `POST /job/{id}/rerun` | odczytuje payload, uruchamia job **od razu, synchronicznie**, zwraca status |
| `POST /job/rerun-all` | to samo dla wszystkich `FAILED`, równolegle na wirtualnych wątkach |

Rerun idzie przez `JobRunner`, który wykonuje job w transakcji `REQUIRES_NEW` i zapisuje wynik w tym
samym wierszu `job_error`. **Nie** wraca przez `scheduled_job` — nie ma ponownego retry ani leasu.

`FailedJobsScheduler` (nocny automatyczny rerun) i `PollScheduler` mają wyłączone `@Scheduled` ze
względów kosztowych — w tej chwili rerun jest wyłącznie ręczny.

---

## Email delivery

```
EventMailJob / AccountInvitationsMailHandler
        │  MailTemplateProcessor.render(...)   → MailContent (temat + HTML)
        ▼
MailDeliveryService.send(type, referenceId, recipients, content)
        │  wiersz email_delivery na odbiorcę
        ▼
MailSender (SmtpMailSender) → SMTP
```

### Warstwy

- `MailTemplateProcessor` — Thymeleaf. Szablony w `resources/templates/mail/`, tematy w
  `messages_*.properties`, lokalizacja z `mail.locale`. Lista szablonów: enum `MailTemplate`.
- `MailSender` — interfejs, w testach podmieniany. `SmtpMailSender` wysyła HTML w UTF-8 z adresu
  `mail.from` / `mail.from-name` i zamienia każdy błąd na `InternalConnectionException`
  (`MAIL_SENDING_FAILED`). Timeouty SMTP: 10 s (`application.yml`).
- `MailDeliveryService` — rejestr dostarczeń. Jedyne miejsce, które powinno wołać `MailSender`.

### Tabela `email_delivery`

Jeden wiersz = jeden mail do jednego odbiorcy w jednej sprawie.

| kolumna | znaczenie |
|---|---|
| `delivery_type` | `EmailDeliveryType`: `USER_CREATION`, `EVENT_CONFIRMED`, `EVENT_GROUP_FORMED`, ... |
| `reference_id` | *sprawa*, której dotyczy mail — id tokenu aktywacyjnego, id wydarzenia, … |
| `user_id` | konto odbiorcy; `null` dla gościa bez konta |
| `email` | adres, na który ostatnio próbowano |
| `status` | `PENDING` → `SENT` / `FAILED` |
| `error_message`, `sent_at` | wynik ostatniej próby |

### Dlaczego można wołać `send` wielokrotnie

`send(type, referenceId, recipients, content)` jest idempotentne względem `(type, referenceId)`:

1. Pobiera wcześniejsze wiersze dla tej pary.
2. Odbiorca rozpoznawany jest po `user_id` (konto — niezależnie od adresu) albo po adresie
   (gość, bez rozróżniania wielkości liter).
3. Kto ma `SENT` — pomijany. Kto ma `PENDING`/`FAILED` — wysyłka ponawiana na *tym samym* wierszu
   (z aktualnym adresem, gdyby konto go zmieniło). Kto nie ma wiersza — dostaje nowy `PENDING`.
4. Każda wysyłka zapisuje wynik od razu (metoda działa poza transakcją).
5. Zwraca liczbę odbiorców, którzy wciąż nie dostali maila.

Wywołujący decyduje, co zrobić z porażkami. `EventMailService` rzuca wyjątek, gdy cokolwiek nie
wyszło — job failuje, `scheduled_job` go ponawia, a kolejna próba pisze **tylko** do tych, którym się
nie udało.

Z tego wynika zasada doboru `reference_id`: **ta sama sprawa = ten sam reference**, inna sprawa = inny.
Przykład: grupa uformowana na termin dwa razy (rozpadła się i zebrała ponownie) to dwie różne
wiadomości, więc `EventMailService` wylicza reference z `(eventId, confirmBy)` przez
`UUID.nameUUIDFromBytes`, a nie używa samego `eventId`.

### Status zaproszeń w panelu

`EmailDeliveryFields.lastDeliveryStatus/lastDelivery` dokładają do listy użytkowników budynku status
ostatniego maila `USER_CREATION` (filtr `invitationStatus` w `BuildingUserFilterRequest`). Manager
widzi, czyje zaproszenie nie doszło, i może zaprosić ponownie — patrz niżej.

---

## Activation token

Konta mieszkańców zakłada manager; mieszkaniec dostaje link i sam ustawia login oraz hasło.

### Zaproszenie

`POST` tworzenia kont w budynku → `PendingAccountService.createPendingAccounts` (jedna transakcja):

1. Normalizuje adresy (trim, lowercase), duplikaty w requeście → `skipped`.
2. Dla każdego adresu:
   - brak konta → nowe konto `CREATED`, rola `RESIDENT`, budynek ze scope, username
     `pending-xxxxxxxxxxxx`, hasło `LOCKED` (nie jest hashem BCrypt, więc nie da się nim zalogować),
   - konto nieaktywne, którego ostatnie zaproszenie nie ma `SENT` → **ponowne zaproszenie**,
   - konto aktywne albo zaproszenie już doręczone → `skipped` z powodem.
3. `ActivationTokenService.issueFor(userIds)` wydaje tokeny.
4. Publikuje `AccountsInvitedEvent` — po commicie handler wysyła maile przez `MailDeliveryService`
   z `delivery_type = USER_CREATION` i `reference_id = id tokenu`.

### Token

- 32 losowe bajty z `SecureRandom`, Base64 URL-safe. Ta wartość trafia **tylko** do linku w mailu.
- W bazie (`activation_token`) jest wyłącznie **SHA-256** tokenu (`token_hash`). Wyciek bazy nie daje
  działających linków.
- Ważność: `activation.token-ttl` (domyślnie 7 dni). Link:
  `{activation.link-base-url}?token=<surowy token>`.
- Ponowne zaproszenie wydaje **nowy** token; stary pozostaje ważny do wygaśnięcia.

### Aktywacja

`PUT /auth/activate` (`token`, `username`, `password`, `firstName`, `lastName`) w jednej transakcji:

1. `consume(token)` — szuka po hashu:
   - brak → `ACTIVATION_TOKEN_INVALID` (400),
   - po `expires_at` → `ACTIVATION_TOKEN_EXPIRED` (400),
   - `UPDATE ... SET used_at = now WHERE used_at IS NULL` zwrócił 0 wierszy → `ACTIVATION_TOKEN_USED`
     (400). Warunkowy update sprawia, że dwa równoległe kliknięcia nie aktywują konta dwa razy.
2. Sprawdza, czy username jest wolny, zapisuje username, hash BCrypt hasła i imię/nazwisko —
   konto staje się aktywne.
3. Zwraca access token (JWT) — użytkownik jest od razu zalogowany. Refresh tokenu ten endpoint nie
   wydaje; pojawi się przy pierwszym `/auth/login`.

---

## Refresh token

### Dwa tokeny

| | access token | refresh token |
|---|---|---|
| format | JWT HS256 (`jwt.secret`), subject = username, claim z rolą | losowy UUID, zapisany w `refresh_token` |
| ważność | `jwt.expiration-ms` | `jwt.refresh-expiration-ms` (dev: 7 dni) |
| gdzie u klienta | body odpowiedzi, frontend wysyła w `Authorization` | ciasteczko `refreshToken`: `HttpOnly`, `SameSite=Strict`, `Path=/`, `Secure` na prod |
| stan po stronie serwera | brak — weryfikacja tylko podpisem i datą | wiersz w bazie, można odwołać |

`buildingId` **nie** jest w JWT — dostęp do budynku sprawdzany jest przy każdym requeście
(patrz `.claude/rules/building_scope.md`).

### Przepływy

**`POST /auth/login`** — weryfikacja username/hasła przez `AuthenticationManager`, nowy refresh token
z IP i User-Agentem klienta, ustawienie ciasteczka, access token w body.

**`POST /auth/refresh`** — rotacja (`RefreshTokenService.rotateRefreshToken`, w transakcji):

1. Szuka tokenu z ciasteczka; brak → `REFRESH_TOKEN_ERROR` (400).
2. Token już użyty albo odwołany → **wykryto ponowne użycie**: odwołuje *wszystkie* aktywne refresh
   tokeny użytkownika (wylogowanie ze wszystkich urządzeń) i odrzuca request. Uczciwy klient nigdy nie
   używa tokenu drugi raz, więc ponowne użycie znaczy, że ktoś go skopiował.
3. Token wygasły → odrzucenie, trzeba się zalogować.
4. Stary token: `is_used = true`, `revoked_at = now`. Wydawany nowy, z nowym IP/User-Agentem.
5. Nowy access token + nowe ciasteczko.

**`POST /auth/logout`** — odwołuje token z ciasteczka (jeśli jest aktywny) i czyści ciasteczko.
Access token działa dalej do swojego wygaśnięcia — nie ma listy odwołanych JWT.

### Znane braki

- Ponowne użycie i wygaśnięcie rzucają surowy `RuntimeException` → klient dostaje generyczne 500
  zamiast 401, wbrew `.claude/rules/exceptions.md`.
- Refresh token leży w bazie jawnym tekstem (w przeciwieństwie do tokenu aktywacyjnego).
- Porównanie IP i User-Agenta ma odwrócone warunki (`if (compareIPs(...))`,
  `if (userAgent.equals(...))`), ale obsługa różnic to puste TODO, więc na razie nie ma to skutków.
  Do poprawy przed wypełnieniem tych metod.
- `/auth/logout` bez ciasteczka kończy się błędem `REFRESH_TOKEN_ERROR` zamiast cichego sukcesu.

---

## Znane ograniczenia infrastruktury

- **Zaproszenia idą przez EventBus**, nie przez `scheduled_job`. Na Cloud Run mogą utknąć albo
  przepaść przy restarcie. Skutek jest naprawialny (status `PENDING`/brak wiersza w
  `email_delivery` → manager zaprasza ponownie), ale nie automatyczny.
- **AsyncJobRunner** (sync z czatem) ma te same problemy i dodatkowo nie zapisuje joba przed
  wykonaniem.
- Job w `scheduled_job` ze statusem `RUNNING`, wygasłym leasem i `attempts = 3` (instancja padła w
  trakcie ostatniej próby) nie jest już przejmowany przez drain i nie trafia do `job_error` — zostaje
  w `RUNNING` na zawsze. Wymaga ręcznej interwencji w bazie.
- Rerun z `job_error` wykonuje job w jednej transakcji (`REQUIRES_NEW`) — dla jobów mailowych to
  trzymanie połączenia z bazą przez cały czas rozmowy z SMTP.
- `JobRerunService` rzuca `RuntimeException` / `IllegalArgumentException` dla nieistniejącego lub
  nieczytelnego wpisu — 500 zamiast 404/400.
- `email_delivery.email` to `VARCHAR(50)` — dłuższy adres nie zmieści się w tabeli.
