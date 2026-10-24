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
- osoby, które zaznaczyły sobotę, dostają maila „zebrała się grupa" z terminem i godziną, do której
  mogą się jeszcze wycofać
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
skasowane, termin decyzji się nie zmienia. Kto dostał maila „zebrała się grupa" i nadal jest zapisany,
dostaje teraz maila „grupa się rozpadła".

Jeśli grupa rozpadnie się, kiedy termin decyzji już minął, na inny termin nie ma już czasu — zostaje
ten, na którym grupa się rozpadła, dokładnie jak w scenariuszu 4, łącznie z mailem „został jeden
termin".

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

- wszyscy zapisani — host też — dostają maila „został jeden termin": który i ilu osób potrzeba
- link do wydarzenia się nie zmienia, ale pokazuje już wyłącznie ten jeden termin — kto wejdzie,
  odpowiada tylko na pytanie, czy ten termin mu pasuje

Kuba wrzuca ten sam link na inną grupę: *„gramy w sobotę o 18, potrzeba 2 ludzi"*. Gdy dojdą dwie
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

## Tryb akceptacji — gdy link trafił do niewłaściwych ludzi

Dotyczy obu trybów wydarzenia — z terminem i bez.

### Problem

Link do wydarzenia jest publiczny: kto go ma, ten może wejść. Zwykle to zaleta, ale link potrafi
wyciec — ktoś przekleja go na większą grupę i nagle w wydarzeniu pojawia się osoba, której host nie
zna albo nie chce.

Host może taką osobę wyrzucić, ale link nadal działa, więc ta osoba może po prostu wejść jeszcze raz.
Wymiana linku na nowy nie wchodzi w grę — trzeba by go od nowa rozesłać wszystkim, którzy już są.

Zamiast zmieniać link, host **zaczyna sam wpuszczać nowych ludzi**.

### Jak to wygląda

```
Kuba widzi na liście kogoś obcego   →  wyrzuca go
aplikacja pyta: „Czy od teraz chcesz akceptować nowe osoby?"
Kuba się zgadza                      →  każdy, kto wejdzie w link, czeka na jego akceptację
```

Tryb akceptacji można też włączyć od razu przy zakładaniu wydarzenia albo później w jego edycji.
Powód nie ma znaczenia — wyrzucenie kogoś to tylko najczęstszy.

**Raz włączonego trybu nie da się wyłączyć.** Gdyby się dało, w wydarzeniu mieszaliby się ludzie,
których host świadomie wpuścił, z tymi, którzy weszli sami w czasie, gdy akceptacja była wyłączona —
i nie byłoby już widać, kto jest kim.

Osoby, które były w wydarzeniu przed włączeniem trybu, zostają. Akceptacja dotyczy tylko nowych.

### Co widzi nowa osoba

Wchodzi w link, podaje imię i adres, tak jak zawsze. Zamiast „jesteś zapisany" widzi
**„czekasz na akceptację hosta"**. Może oglądać wydarzenie, ale nie może się zapisać ani zrezygnować,
dopóki host jej nie wpuści.

W wydarzeniu bez terminu może od razu zaznaczyć, które terminy jej pasują — ale **jej odpowiedź nie
liczy się do progu**, dopóki host jej nie wpuści. Grupa nie może się zebrać dzięki komuś, kogo host
jeszcze nie zna.

### Co widzi host

Osobną listę **„czekają na akceptację"**, ułożoną w kolejności zgłoszeń. Może przy niej:

- **wpuścić kilka osób naraz**
- **wyrzucić kilka osób naraz** — zarówno czekających, jak i tych, którzy już są w wydarzeniu

### Wpuszczanie idzie po kolei

Host decyduje, **ile** osób wpuścić, ale nie **kto pierwszy**. Wpuścić można tylko pierwsze osoby z
kolejki — jeśli czekają Anna, Bartek i Celina, host może wpuścić samą Annę, Annę z Bartkiem albo
wszystkich troje, ale nie samego Bartka przed Anną.

Powód jest prosty: wpuszczenie rozdaje miejsca. Gdyby host mógł wybierać dowolnie, „kto pierwszy, ten
lepszy" przestałoby obowiązywać, a ktoś, kto zgłosił się wcześniej, mógłby zostać bez miejsca przez
to, że host wolał kogoś innego. Jeśli host nie chce wpuścić pierwszej osoby z kolejki, wyrzuca ją —
wtedy kolejna staje się pierwsza.

### Co dostaje wpuszczona osoba

| sytuacja | po wpuszczeniu |
|---|---|
| wydarzenie bez terminu, termin jeszcze nieustalony | jest w grupie, jej wybór terminów zaczyna się liczyć |
| wydarzenie z terminem albo termin już potwierdzony, są wolne miejsca | jest zapisana |
| brak miejsc, jest lista rezerwowa | trafia na listę rezerwową |
| brak miejsc i brak listy rezerwowej | jest w wydarzeniu, ale bez miejsca — może się zapisać, gdy coś się zwolni |

Osoby wpuszczone za jednym razem zachowują między sobą kolejność zgłoszeń — także na liście
rezerwowej.

W wydarzeniu bez terminu wpuszczenie ludzi może sprawić, że któryś termin osiągnie próg. Wtedy grupa
powstaje dokładnie tak, jak w scenariuszu 1.

### Wyrzucanie

Wyrzucona osoba traci wszystko, co miała w wydarzeniu: miejsce, swoje odpowiedzi i zalogowanie na
każdym urządzeniu. Jej kod powrotu przestaje działać.

Skutki dla reszty są takie same, jak gdyby ta osoba sama zrezygnowała:

- zwolnione miejsce dostaje pierwsza osoba z listy rezerwowej
- w wydarzeniu bez terminu, w którym grupa właśnie się zebrała, wyrzucenie kogoś może zbić liczbę
  osób poniżej progu — wtedy wydarzenie wraca do zbierania odpowiedzi (scenariusz 3)

Hosta nie da się wyrzucić z jego własnego wydarzenia.

> **Wyrzucenie samo w sobie nie zamyka drzwi.** Link dalej działa, więc wyrzucona osoba może wejść
> ponownie. Bez trybu akceptacji wróci od razu, z trybem — stanie na końcu kolejki i host znów
> zdecyduje. Dlatego zaraz po wyrzuceniu kogoś aplikacja proponuje włączenie trybu akceptacji.

### Edycja wydarzenia

Host może zmienić nazwę, opis, miejsce i link do spotkania online, a także włączyć tryb akceptacji.
Daty, liczby miejsc, progu ani trybu wydarzenia zmienić się nie da — od nich zależy, kto już ma
miejsce, a zmienianie ich ludziom pod nogami to osobny temat.

---

## Powiadomienia

Powiadomienia przychodzą mailem. Dlatego każdy, kto dołącza do wydarzenia bez konta, podaje adres.
Osoba z kontem Unite dostaje maila na adres z konta.

Maile dotyczą tylko wydarzeń bez terminu — wydarzenie z terminem nie wysyła żadnych. Host nie ma
osobnych maili: dostaje te same co pozostali, bo sam jest zapisany i pasują mu wszystkie terminy,
które zaproponował.

| kiedy | kto dostaje | co mówi |
|---|---|---|
| **zebrała się grupa** | zapisani, którzy zaznaczyli wybrany termin — jako „pasuje mi" albo „dam radę, jeśli trzeba" | który termin wyszedł i do kiedy można się wycofać |
| **grupa się rozpadła** | ci sami co wyżej, o ile nadal są zapisani | że termin stracił komplet, wszystkie terminy znów są otwarte i do kiedy |
| **termin potwierdzony** | ci, którzy dostali miejsce | że wydarzenie się odbędzie, i kiedy |
| **został jeden termin** | wszyscy zapisani | który termin został i ilu osób potrzeba — oraz dlaczego: minął termin decyzji albo grupa rozpadła się już po nim |

Każdy mail zawiera nazwę wydarzenia, termin, miejsce i link do wydarzenia.

Grupa może się zebrać, rozpaść i zebrać ponownie — każde zebranie i każdy rozpad to osobny mail.
To samo dotyczy maila „został jeden termin", gdy grupa na ostatnim terminie rozpada się więcej niż
raz.

Do maila „termin potwierdzony" brane są osoby **po** rozdaniu miejsc. Nie dostają go ci, którym
wybrany termin nie pasował, ani ci, którzy nie zmieścili się w limicie i trafili na listę rezerwową.

---

## Czego jeszcze nie ma

**Część sytuacji nie wysyła żadnego maila**, choć ktoś mógłby chcieć się o nich dowiedzieć:

- termin poszedł w inną stronę — osoby, którym wybrany termin nie pasował, nie dostają informacji
- lista rezerwowa — ani przy trafieniu na nią, ani gdy zwolni się miejsce
- tryb akceptacji — host nie dostaje maila, gdy ktoś czeka na wpuszczenie, a czekający — gdy go
  wpuszczono albo wyrzucono

Mail, którego nie udało się wysłać, nie jest ponawiany.

Poza zakresem na teraz: anulowanie wydarzenia przez hosta i możliwość nadpisania przez hosta terminu
wybranego przez system.
