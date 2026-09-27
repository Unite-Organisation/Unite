# Wydarzenia, które same ustalają termin

Dokumentacja biznesowa. Opisuje, jak działa mechanizm z perspektywy ludzi, którzy z niego
korzystają — hosta i uczestników.

---

## Problem

Dotychczas wydarzenie publiczne działało w jeden sposób: organizator miał termin i szukał do niego
ludzi. Wystarczyło wejść w link i się zapisać, a gdy chętnych było więcej niż miejsc, nadmiar trafiał
na listę rezerwową.

To nie pokrywa bardzo częstej sytuacji. Ktoś chce zagrać w siatkówkę. Wie, **z kim** chce zagrać i
**ilu** ludzi potrzeba, żeby w ogóle było warto — ale nie wie **kiedy**, bo to zależy od tego, komu
kiedy pasuje. Ustalanie terminu przenosi się wtedy na czat, gdzie ginie w trzydziestu wiadomościach
„a mi pasuje wtorek albo czwartek, ale czwartek tylko po 19".

Nowy tryb przenosi to ustalanie do aplikacji.

---

## Dwa tryby wydarzenia

Organizator wybiera tryb przy zakładaniu wydarzenia i nie zmienia go już później.

### Tryb z terminem

Dokładnie to, co było do tej pory. Host zna datę, wpisuje ją, wysyła link. Ludzie wchodzą i
zapisują się. Jeśli jest limit miejsc, nadmiar idzie na listę rezerwową.

Nic się w tym trybie nie zmieniło.

### Tryb bez terminu

Host nie zna daty. Zamiast niej podaje **propozycje terminów** i mówi, **ilu ludzi** potrzeba, żeby
wydarzenie miało sens. Termin wyłania się z odpowiedzi uczestników.

---

## Co podaje host, zakładając wydarzenie bez terminu

| co | przykład | po co |
|---|---|---|
| nazwa | „Siatkówka" | |
| propozycje terminów | sobota 18:00, niedziela 12:00, wtorek 20:00, czwartek 19:00 | od 2 do 6 |
| próg | 6 osób | poniżej tej liczby wydarzenie się nie odbywa |
| termin decyzji | piątek 20:00 | do tego momentu trzeba wiedzieć |
| limit miejsc (opcjonalnie) | 12 osób | |

**Termin decyzji** to moment, w którym host musi już znać datę — bo np. rezerwuje halę albo kupuje
bilety. Musi wypaść przed najwcześniejszą z proponowanych dat.

Host jest automatycznie liczony jako dostępny we **wszystkich** terminach, które sam zaproponował.
Skoro je wpisał, to znaczy, że mu pasują.

---

## Co robi uczestnik

Wchodzi w link i widzi kafelki z terminami. Odpowiada w dwóch krokach:

**Krok 1 — „te terminy mi pasują"**
Zaznacza wszystkie daty, w których realnie może być.

**Krok 2 — „te dam radę, jeśli od tego zależy, czy się zbierzemy"**
Spośród pozostałych zaznacza te, które są dla niego gorsze, ale możliwe, jeśli inaczej grupa się nie
uzbiera. To krok opcjonalny — można zaznaczyć jeden termin w pierwszym kroku i nie zaznaczać już nic.

Następnie klika „dołącz". Wybór zapisuje się razem z dołączeniem — nie ma stanu „jestem w grupie,
ale jeszcze nic nie powiedziałem".

Uczestnik może później zmienić zdanie i przesłać wybór od nowa, dopóki termin nie został ustalony.

### Dlaczego dwa rodzaje odpowiedzi

To jest sedno całego mechanizmu. Bez drugiego kroku ludzie albo zaznaczaliby tylko idealne terminy
(i nic by się nie zbierało), albo wszystkie możliwe (i wychodziłby termin, którego nikt nie chciał).

Rozdzielenie odpowiedzi pozwala systemowi **najpierw spróbować ułożyć grupę z samych chętnych**, a
dopiero gdy się nie da — sięgnąć po tych, którzy się poświęcą.

---

## Jak system wybiera termin

Reguła jest celowo prosta, żeby dało się ją wytłumaczyć uczestnikowi jednym zdaniem:

> **Wygrywa termin, w którym zbierze się dość osób, dla których ten termin jest po prostu dobry.
> Dopiero gdy żaden taki nie istnieje, bierzemy pod uwagę deklaracje „dam radę, jeśli trzeba".**

Na przykładzie. Próg wynosi 6 osób:

| termin | „pasuje mi" | „dam radę, jeśli trzeba" | razem |
|---|---|---|---|
| sobota | 6 | 0 | 6 |
| niedziela | 4 | 5 | 9 |

**Wygrywa sobota**, mimo że w niedzielę byłoby więcej ludzi. Sobota zbiera komplet z samych chętnych,
niedziela musi dobrać ludzi, którzy woleliby inaczej.

Gdyby żaden termin nie spinał się samymi „pasuje mi", system bierze te, które spinają się z dobitką,
i wybiera ten z największą liczbą pewnych deklaracji. Przy remisie — ten z większą liczbą osób, a na
końcu ten wcześniejszy.

### Co widać na kafelku

Każdy termin pokazuje, ile osób deklaruje każdy rodzaj dostępności, oraz **wskaźnik zapełnienia** —
jak blisko jest do progu. Deklaracja „dam radę, jeśli trzeba" liczy się do tego wskaźnika za pół,
bo jest mniej pewna.

Kafelki są **posortowane od najbliższego do zebrania**, więc na górze zawsze stoi termin, który
wygrałby w tej chwili.

---

## Scenariusze

### 1. Grupa zbiera się przed terminem decyzji

Najczęstszy przebieg.

```
poniedziałek   Kuba zakłada wydarzenie, wysyła link na grupę
wtorek–czwartek ludzie zaznaczają terminy i dołączają
czwartek 21:00  sobota osiąga 6 osób  →  GRUPA POWSTAJE
```

W momencie zebrania progu:

- termin zostaje **wybrany**, pozostałe propozycje znikają
- wszyscy dostają powiadomienie „grupa powstała, gramy w sobotę o 18:00"
- rusza **godzinne okno na wycofanie się**

Okno istnieje po to, żeby ludzie, którzy zaznaczyli sobotę jako gorszą opcję, mieli szansę się
wycofać, gdy okaże się, że to właśnie ona wyszła. Jeśli nikt nie wyjdzie, po godzinie wydarzenie jest
ostatecznie umówione.

Od tego momentu jest to zwykłe wydarzenie z terminem — wygląda i działa tak samo jak wydarzenie
założone w trybie z terminem.

### 2. Host nie chce czekać godziny

Kuba widzi, że grupa się zebrała, i wie, że musi już rezerwować halę.

Klika „uruchom teraz" i wydarzenie jest ostatecznie umówione bez czekania na koniec okna. Dostępne
tylko dla hosta.

### 3. Ktoś wycofuje się w oknie i grupy zabraknie

```
czwartek 21:00  sobota osiąga 6 osób  →  grupa powstaje, rusza okno
czwartek 21:10  Anna się wycofuje      →  zostaje 5 osób
```

Wydarzenie **wraca do zbierania odpowiedzi**. Wszystkie terminy znów są dostępne, okno zostaje
skasowane, termin decyzji się nie zmienia.

To wynika wprost z tego, co powiedział host: „6 osób albo nie ma sensu". Próg jest warunkiem
istnienia grupy, a nie bramką, przez którą wystarczy raz przejść. Jeśli do piątku dojdzie ktoś nowy
i znów będzie 6 osób, okno startuje od nowa.

### 4. Termin decyzji mija, a grupy nie ma

```
piątek 20:00  mija termin decyzji
              najlepszy termin (sobota) ma 4 osoby z 6
```

Wydarzenie nie umiera. System **wybiera jeden termin** — ten z najlepszym poparciem — i odrzuca
pozostałe. Od tej chwili:

- Kuba dostaje powiadomienie: „nie zebrało się, najlepszy termin to sobota 18:00, brakuje 2 osób"
- dostaje też **link do dobierania ludzi na ten konkretny termin** — bez kafelków, z jedną datą i
  pytaniem „jesteś?"
- stary link z grupy nadal działa, tylko pokazuje już wyłącznie ten jeden termin

Kuba wrzuca nowy link na inną grupę: *„gramy w sobotę o 18, potrzeba 2 ludzi"*. Gdy dojdą dwie
osoby, grupa powstaje normalnie — z oknem na wycofanie się i całą resztą.

**Odrzucone terminy są odrzucone nieodwracalnie.** Nawet gdyby po terminie decyzji zgłosiło się
pięć osób chętnych na wtorek, wtorku już nie ma. Inaczej termin decyzji nie znaczyłby nic.

### 5. Chętnych jest więcej niż miejsc

Limit miejsc zaczyna działać dopiero **po ustaleniu terminu**, i to z prostego powodu: grupa powstaje
dokładnie w momencie osiągnięcia progu, więc przed ustaleniem nigdy nie ma więcej chętnych, niż host
wymagał.

Wydarzenie ma próg 6 osób i limit 12 miejsc. Gdy zbierze się 6 osób i termin zostaje potwierdzony,
wydarzenie staje się zwykłym wydarzeniem z terminem — i od tej chwili **przyjmuje normalne zapisy**,
bez kafelków i bez wybierania dat. Kolejne osoby wchodzą w link i po prostu się zapisują, aż do
wyczerpania dwunastu miejsc. Kto się nie zmieści, trafia na listę rezerwową, a gdy ktoś zrezygnuje —
pierwsza osoba z listy dostaje zwolnione miejsce.

W praktyce próg i limit odpowiadają więc na dwa różne pytania: **próg** mówi, ile osób musi być,
żeby w ogóle zaczynać, a **limit** — ile osób się ostatecznie zmieści.

> **W godzinnym oknie na wycofanie się nowi ludzie nie dołączają.** Okno służy wyłącznie temu, żeby
> ci, którzy już są, mogli się wycofać. Kto nie zdążył, zapisuje się po potwierdzeniu terminu.

### 6. Komuś nie pasował wybrany termin

Grzegorz zaznaczył wtorek i środę. Wyszła sobota.

Grzegorz **nie jest zapisany** na wydarzenie, ale system odnotowuje to jako *„termin Ci nie pasował"*,
a nie *„zrezygnowałeś"*. To rozróżnienie jest widoczne dla niego i dla hosta — Grzegorz nigdy nie
powiedział „nie", po prostu data poszła w inną stronę.

### 7. Wydarzenie z terminem

Bez zmian. Host podaje datę, ludzie się zapisują, nadmiar idzie na listę rezerwową. Żadnych kafelków,
żadnego progu, żadnego terminu decyzji.

---

## Co widać na ekranie wydarzenia

**Tryb z terminem** — lista uczestników i ewentualna lista rezerwowa.

**Tryb bez terminu, przed ustaleniem** — kafelki z terminami, posortowane od najbliższego do
zebrania, każdy z liczbą deklaracji i wskaźnikiem zapełnienia. Plus własny wybór uczestnika.

**Tryb bez terminu, po ustaleniu** — ekran wygląda jak wydarzenie z terminem, bo nim się właśnie
stało.

---

## Stany wydarzenia

| stan | co znaczy |
|---|---|
| **zbieranie odpowiedzi** | wszystkie terminy w grze, ludzie deklarują dostępność |
| **został jeden termin** | minął termin decyzji, grupy nie było, dobieramy ludzi na jedną datę |
| **grupa powstała** | termin wybrany, trwa godzinne okno na wycofanie się |
| **potwierdzone** | data ostateczna, miejsca rozdane |

---

## Czego jeszcze nie ma

**Powiadomienia są zamockowane.** Cała logika rozpoznaje, kiedy wysłać wiadomość („grupa powstała",
„wybraliśmy termin", „potwierdzone") i robi to w odpowiednich momentach — ale na razie zapisuje to
w logach zamiast wysyłać. Uczestnik wydarzenia publicznego nie musi mieć konta ani podanego adresu,
więc trzeba najpierw rozstrzygnąć, czym w ogóle się z nim kontaktować.

Poza zakresem na teraz: anulowanie wydarzenia przez hosta i możliwość nadpisania przez hosta terminu
wybranego przez system.
