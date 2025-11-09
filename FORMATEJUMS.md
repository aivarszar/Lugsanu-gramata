# Convocatis Tekstu Formatējuma Specifikācija

Šis dokuments apraksta, kā formatēt tekstus Convocatis programmā, izmantojot speciālos kodus un HTML tagus.

## Satura rādītājs

1. [Pamata formatējuma kodi](#pamata-formatējuma-kodi)
2. [HTML tagi](#html-tagi)
3. [Attēli](#attēli)
4. [PDF faili](#pdf-faili)
5. [Ārējās saites](#ārējās-saites)
6. [Piemēri](#piemēri)

---

## Pamata formatējuma kodi

### 1. Lapas atdalītājs: `|`

Simbols `|` (vertikālā svītra) sadala tekstu atsevišķās lapās, pa kurām var pārvietoties ar swipe vai navigācijas pogām.

**Piemērs:**
```
Pirmā lapa|Otrā lapa|Trešā lapa
```

### 2. Virsraksti: `>>teksts<<`

Virsraksts tiek apzīmēts ar `>>` un `<<` simboliem. Virsraksts tiek attēlots augšējā sekcijā un rada jaunu navigācijas līmeni.

**Piemērs:**
```
>>Lūgšana par mieru<<
Kungs, dāvā mums mieru...
```

**Rezultāts:**
- Augšējā sekcijā: "Lūgšana par mieru" (kā virsraksts)
- Apakšējā sekcijā: teksta saturs

### 3. Atkārtojumi: `N^teksts`

Atkārto tekstu N reizes. Noderīgs, piemēram, rožukroņa lūgšanām.

**Piemērs:**
```
10^Sveika, Marija, žēlastības pilnā...
```

**Rezultāts:** Teksts tiks atkārtots 10 reizes, katrs atkārtojums būs atsevišķa lapa ar progresa rādītāju (1/10, 2/10, utt.).

### 4. Atsauces uz citiem tekstiem: `%RID`

Iekļauj cita teksta saturu pēc tā RID (unikālā identifikatora).

**Piemērs:**
```
%123
```

**Rezultāts:** Teksta vietā tiks ievietots teksts ar RID 123.

### 5. Kombinētie kodi: `N^%RID`

Atkārto citu tekstu N reizes.

**Piemērs:**
```
>>Prieka noslēpumi<<
10^%456
```

**Rezultāts:** Teksts ar RID 456 tiks atkārtots 10 reizes zem virsraksta "Prieka noslēpumi".

### 6. Dzēšanas atzīme: `--delete-`

Ja teksta Description laukā sākas ar `--delete-`, šis teksts netiks ielādēts datubāzē. Noderīgs, lai pagaidām paslēptu tekstus, nezaudējot tos no XML faila.

**Piemērs XML failā:**
```xml
<item>
  <RID>999</RID>
  <Description>--delete-Vecs teksts</Description>
  <String>Šis teksts netiks rādīts</String>
</item>
```

---

## HTML tagi

Tekstu saturā var izmantot HTML tagus formatēšanai. Programma atbalsta Android `HtmlCompat` bibliotēku, kas pārvērš HTML tagus vizuālajā formatējumā.

### Atbalstītie HTML tagi:

#### Teksta formatējums

| Tags | Apraksts | Piemērs |
|------|----------|---------|
| `<b>teksts</b>` | **Treknraksts** | `<b>Svarīgi!</b>` |
| `<i>teksts</i>` | *Slīpraksts* | `<i>Amen</i>` |
| `<u>teksts</u>` | <u>Pasvītrots</u> | `<u>Pamatzīme</u>` |
| `<small>teksts</small>` | Mazāks teksts | `<small>Piezīme</small>` |
| `<big>teksts</big>` | Lielāks teksts | `<big>Virsraksts</big>` |
| `<sup>teksts</sup>` | Augšraksts | `2<sup>nd</sup>` |
| `<sub>teksts</sub>` | Apakšraksts | `H<sub>2</sub>O` |

#### Strukturālie tagi

| Tags | Apraksts | Piemērs |
|------|----------|---------|
| `<h1>teksts</h1>` | Virsraksts 1. līmeņa | `<h1>Galvenais virsraksts</h1>` |
| `<h2>teksts</h2>` | Virsraksts 2. līmeņa | `<h2>Apakšvirsraksts</h2>` |
| `<h3>teksts</h3>` | Virsraksts 3. līmeņa | `<h3>Mazāks virsraksts</h3>` |
| `<p>teksts</p>` | Rindkopa | `<p>Jauna rindkopa</p>` |
| `<br>` vai `<br/>` | Jaunas rindas pārtraukums | `Pirmā rinda<br>Otrā rinda` |
| `<div>teksts</div>` | Bloka elements | `<div>Saturs</div>` |
| `<span>teksts</span>` | Inline elements | `<span>Teksts</span>` |

#### Saraksti

| Tags | Apraksts | Piemērs |
|------|----------|---------|
| `<ul><li>...</li></ul>` | Nesakārtots saraksts | `<ul><li>Punkts 1</li><li>Punkts 2</li></ul>` |
| `<ol><li>...</li></ol>` | Sakārtots saraksts | `<ol><li>Pirmais</li><li>Otrais</li></ol>` |

#### Krāsas un stils

| Tags | Apraksts | Piemērs |
|------|----------|---------|
| `<font color="#FF0000">teksts</font>` | Krāsains teksts | `<font color="#FF0000">Sarkans teksts</font>` |
| `<font color="red">teksts</font>` | Krāsains teksts (nosaukums) | `<font color="blue">Zils teksts</font>` |

### HTML entītijas

Lai attēlotu speciālus simbolus, kas tiek izmantoti kodēšanā:

| Entītija | Simbols | Apraksts |
|----------|---------|----------|
| `&lt;` | < | Mazāk par |
| `&gt;` | > | Lielāks par |
| `&amp;` | & | Ampersands |
| `&quot;` | " | Pēdiņas |
| `&gt;&gt;` | >> | Divkāršs lielāks par |
| `&lt;&lt;` | << | Divkāršs mazāk par |

**Piemērs:**
```
&gt;&gt;Šis nav virsraksts&lt;&lt;
```

**Rezultāts:** `>>Šis nav virsraksts<<` (parādīts kā teksts, nevis virsraksts)

---

## Attēli

Programma atbalsta attēlu iekļaušanu, izmantojot HTML `<img>` tagu.

### Attēlu avoti

1. **URL no interneta:**
```html
<img src="https://example.com/image.jpg" width="300" height="200" />
```

2. **Lokālie resursi no `drawable/` mapes:**
```html
<img src="ic_cross" />
```

3. **Assets mape:**
```html
<img src="file:///android_asset/images/prayer.jpg" />
```

### Attēla atribūti

| Atribūts | Apraksts | Piemērs |
|----------|----------|---------|
| `src` | Attēla avots (obligāts) | `src="image.jpg"` |
| `width` | Platums pikseļos | `width="300"` |
| `height` | Augstums pikseļos | `height="200"` |
| `alt` | Alternatīvs teksts | `alt="Krusts"` |

### Piemēri

**1. Vienkāršs attēls:**
```html
<img src="https://example.com/cross.png" />
```

**2. Attēls ar izmēriem:**
```html
<img src="https://example.com/madonna.jpg" width="400" height="600" alt="Dievmāte Marija" />
```

**3. Teksts ar attēlu:**
```html
<h2>Svētā Krusts</h2>
<img src="https://example.com/cross.png" width="200" height="300" />
<p>Lūgšana pie Svētā Krusta...</p>
```

---

## PDF faili

Programma var atsaukties uz PDF failiem, izmantojot saites. PDF tiks atvērts sistēmas PDF skatītājā.

### PDF saites sintakse

```html
<a href="file:///android_asset/documents/prayer_book.pdf">Lūgšanu grāmata (PDF)</a>
```

vai

```html
<a href="https://example.com/documents/songs.pdf">Dziesmu grāmata (PDF)</a>
```

### PDF failu izmantošana

1. **Lokālie PDF faili:**
   - Ievietojiet PDF failus `app/src/main/assets/documents/` mapē
   - Atsaucieties uz tiem ar `file:///android_asset/documents/filename.pdf`

2. **Ārējie PDF faili:**
   - Izmantojiet pilnu URL: `https://example.com/file.pdf`

### Piemērs

```html
<h2>Resursi</h2>
<p>Pilna lūgšanu grāmata pieejama šeit:</p>
<a href="file:///android_asset/documents/prayer_book.pdf">Lejupielādēt PDF</a>
```

---

## Ārējās saites

Programma atbalsta ārējo saišu iekļaušanu, kas tiks atvērtas pārlūkprogrammā.

### Saišu sintakse

```html
<a href="URL">Saites teksts</a>
```

### Piemēri

**1. Vienkārša saite:**
```html
<a href="https://www.vatican.va">Vatikāna mājaslapa</a>
```

**2. Saite uz konkrētu lapu:**
```html
<a href="https://www.catholic.lv/lv/leksana-teksti">Lasījumu teksti</a>
```

**3. E-pasta saite:**
```html
<a href="mailto:info@example.com">Rakstīt e-pastu</a>
```

**4. Tālruņa saite:**
```html
<a href="tel:+37112345678">Zvanīt: +371 12345678</a>
```

### Saites ar attēliem

```html
<a href="https://www.vatican.va">
  <img src="https://example.com/vatican_logo.png" width="100" height="100" />
  <br>Vatikāna mājaslapa
</a>
```

---

## Piemēri

### 1. Vienkārša lūgšana ar formatējumu

```
>>Tēvs Mūsu<<
<b>Tēvs Mūsu</b>, kas esi debesīs,<br>
svētīts lai top <i>Tavs vārds</i>.<br>
Lai nāk Tava valstība,<br>
<u>lai notiek Tavs prāts</u>, kā debesīs, tā arī virs zemes.<br>
<small>Amen</small>
```

### 2. Rožukronis ar atkārtojumiem

```
>>Prieka noslēpumi<<
Piedāvājam Tev pirmo prieka noslēpumu...<br>
Tēvs Mūsu: %101|10^%102|%103
```

Kur:
- RID 101 = Tēvs Mūsu teksts
- RID 102 = Sveika, Marija teksts
- RID 103 = Gods Tēvam teksts

### 3. Dziesma ar attēlu un saiti

```
>>Lūgšana pie Svētā Krusta<<
<img src="https://example.com/holy_cross.jpg" width="300" height="400" />
<br><br>

<h3>Lūgšana</h3>
<p>Ak, <b>svētais Kungs Jēzu</b>, mēs lūdzamies pie Tava krusta...</p>

<p><small>Avots: <a href="https://www.catholic.lv">Catholic.lv</a></small></p>
```

### 4. Teksts ar PDF pielikumu

```
>>Bībeles lasījumi<<
<h2>Svētdienas lasījumi</h2>

<p>Šīs nedēļas lasījumus varat izlasīt zemāk vai lejupielādēt PDF formātā:</p>

<a href="file:///android_asset/documents/readings.pdf">Lejupielādēt lasījumus (PDF)</a>

<hr>
<h3>Pirmais lasījums</h3>
<p>Lasījums no Jesajas grāmatas...</p>
```

### 5. Kontaktu lapa ar e-pastu un tālruni

```
>>Kontakti<<
<h2>Sazinies ar mums</h2>

<p>Ja Tev ir jautājumi vai ieteikumi, lūdzu, sazinies ar mums:</p>

<ul>
  <li><b>E-pasts:</b> <a href="mailto:aivarszar@gmail.com">aivarszar@gmail.com</a></li>
  <li><b>Tālrunis:</b> <a href="tel:+37112345678">+371 12345678</a></li>
  <li><b>Mājaslapa:</b> <a href="https://convocatis.lv">convocatis.lv</a></li>
</ul>
```

### 6. Krāsains teksts ar izcēlumiem

```
>>Svētā Gara lūgšana<<
<h2 style="color: red;">Nāc, Svētais Gars!</h2>

<p><font color="#0000FF">Nāc, Svētais Gars</font>, piepildi savu ticīgo sirdis,<br>
un <font color="red">iededzini viņos savas mīlestības uguni</font>.</p>

<p><b>V.</b> Sūti savu Garu, un viss taps radīts.<br>
<b>A.</b> Un Tu atjaunosi zemes vaigu.</p>
```

### 7. Vairāku lapu teksts ar virsrakstiem

```
>>Ievads<<
Šī ir dziesmu grāmata ar vairākiem virsrakstiem.|

>>Pirmā dziesma<<
Teksts pirmajā dziesmai...|

>>Otrā dziesma<<
Teksts otrajā dziesmai...|

>>Noslēgums<<
Paldies, ka izmantojāt šo grāmatu.
```

---

## Labākās prakses

### 1. Formatējuma konsistence
- Izmantojiet vienādus HTML tagus visā tekstā
- Saglabājiet konsekventu atkāpi un struktūru

### 2. Attēlu optimizācija
- Izmantojiet optimizētus attēlus (JPEG vai PNG)
- Norādiet attēla izmērus, lai izvairītos no pārmērīga lieluma

### 3. Saišu drošība
- Izmantojiet HTTPS saites drošībai
- Pārbaudiet, vai saites ir spēkā

### 4. Testēšana
- Vienmēr testējiet tekstu programmā pirms publicēšanas
- Pārbaudiet, vai visi atkārtojumi darbojas pareizi
- Pārbaudiet, vai atsauces (%RID) norāda uz pareiziem tekstiem

### 5. Pieejamība
- Izmantojiet `alt` atribūtu attēliem
- Nodrošiniet skaidrus saišu aprakstus
- Izmantojiet semantiskus HTML tagus (h1, h2, p, utt.)

---

## Bieži sastopamās kļūdas

### 1. Nepareizi virsraksti
❌ **Nepareizi:** `>>Virsraksts<`
✅ **Pareizi:** `>>Virsraksts<<`

### 2. Aizmirsts atdalītājs
❌ **Nepareizi:** `Pirmā lapаOtrā lapa`
✅ **Pareizi:** `Pirmā lapa|Otrā lapa`

### 3. Nepareiza atsauce
❌ **Nepareizi:** `%abc` (RID nav skaitlis)
✅ **Pareizi:** `%123`

### 4. Nepareiza HTML sintakse
❌ **Nepareizi:** `<b>Teksts<b>` (nav slēgts tags)
✅ **Pareizi:** `<b>Teksts</b>`

### 5. Nepareizi atkārtojumi
❌ **Nepareizi:** `10xTeksts`
✅ **Pareizi:** `10^Teksts`

---

## Papildu resursi

- **Android HtmlCompat dokumentācija:** https://developer.android.com/reference/androidx/core/text/HtmlCompat
- **HTML pamati:** https://www.w3schools.com/html/
- **Krāsu kodi:** https://htmlcolorcodes.com/

---

**Piezīme:** Šī specifikācija tiek regulāri atjaunināta. Ja Tev ir jautājumi vai ieteikumi, lūdzu, sazinies ar izstrādātāju: aivarszar@gmail.com

**Versija:** 1.0
**Pēdējās izmaiņas:** 2025-10-24
