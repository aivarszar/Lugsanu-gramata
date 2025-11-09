# Kustību Skaņas / Motion Sounds

Android aplikācija, kas atskaņo dažādus komiskus skaņu efektus atbilstoši ierīces kustībai.

## 📱 Apraksts

Šī aplikācija izmanto Android ierīces accelerometer sensoru, lai detektētu dažādas kustības un atskaņotu atbilstošus skaņu efektus. Katra kustība aktivizē unikālu, jautru skaņu!

## 🎯 Kustības un skaņas

| Kustība | Apraksts | Skaņa |
|---------|----------|-------|
| **Ādas kasīšana** | Cili ierīci uz augšu un uz leju | Ādas kasīšanas troksnis |
| **Šūpoles** | Šūpo ierīci pa labi un pa kreisi, vienlaikus ceļot un nolaižot (kā šūpolēs) | Čīkstoša šūpole |
| **OHO!** | Met ierīci uz augšu un noķer to | "OHO" sauciens |
| **Būkšķis** | Nomet ierīci uz leju (uzmanīgi!) | Būkšķis |
| **Švīkstoņa** | Šūpini ierīci ar īso malu uz leju | Švīkstēšanas skaņa |

## ✨ Funkcionalitāte

### ✅ Implementēts:

1. **Sensoru detekcija**
   - Accelerometer sensora izmantošana
   - Real-time kustību analīze
   - Precīza kustību atpazīšana

2. **Skaņu atskaņošana**
   - SoundPool integrācija
   - Vairāku skaņu vienlaicīga atskaņošana
   - Optimizēta audio performance

3. **Lietotāja interfeiss**
   - Material Design komponenti
   - Real-time sensoru datu attēlošana
   - Kustības stāvokļa indikatori
   - Krāsu kodēti stāvokļi

4. **Jutīguma kontrole**
   - 3 jutīguma līmeņi (Zems, Vidējs, Augsts)
   - Dinamiski pielāgojami sliekšņi
   - Personalizēta lietošanas pieredze

5. **Kustību detektori**
   - Vertikāla cilāšana (scratching)
   - Šūpošanās kustība (swinging)
   - Mešana uz augšu (throwing)
   - Nomešana (dropping)
   - Švīkstoņa (whooshing)

## 🔧 Tehniskais steks

- **Valoda**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Sensori**: Accelerometer (obligāts), Gyroscope (optional)
- **Audio**: SoundPool API
- **UI**: Material Design Components, ViewBinding
- **Dependencies**: AndroidX, ConstraintLayout

## 📁 Projekta struktūra

```
app/src/main/
├── java/com/motionsounds/app/
│   └── MainActivity.kt          # Galvenā aktivitāte ar sensoru loģiku
├── res/
│   ├── layout/
│   │   └── activity_main.xml    # UI layout
│   ├── values/
│   │   ├── strings.xml          # Tekstu resursi
│   │   ├── colors.xml           # Krāsu definīcijas
│   │   └── themes.xml           # Tēmas
│   └── raw/                     # Skaņu faili (pievienojami)
└── AndroidManifest.xml
```

## 🎵 Skaņu failu pievienošana

Lai pievienotu skaņu efektus:

1. Sagatavo 5 audio failus (MP3, OGG, vai WAV formātā):
   - `scratching.mp3` - ādas kasīšana
   - `swinging.mp3` - šūpoles
   - `oho.mp3` - OHO sauciens
   - `thud.mp3` - būkšķis
   - `whoosh.mp3` - švīkstoņa

2. Ievieto failus `app/src/main/res/raw/` direktorijā

3. Atkomentē `loadSounds()` funkciju `MainActivity.kt:85-91`

## 🚀 Uzstādīšana un palaišana

1. Klonē repozitoriju:
```bash
git clone <repository-url>
cd Lugsanu-gramata
```

2. Atver projektu Android Studio

3. Sync Gradle failus

4. Pievieno skaņu failus (skatīt augstāk)

5. Palaid uz Android ierīces vai emulātora

## ⚠️ Piezīmes

- Aplikācija darbojas tikai uz fiziskām ierīcēm ar accelerometer sensoru
- Emulātoram ir ierobežota sensoru simulācija
- Ieteicams testēt uz īstām ierīcēm
- **Uzmanību**: Esiet piesardzīgi, testējot "nomešanas" kustību!

## 📝 Nākotnes uzlabojumi

- [ ] Pievienot īstas skaņu efektus
- [ ] Implementēt vibrācijas feedback
- [ ] Pievienot skaņu skaļuma kontroli
- [ ] Saglabāt lietotāja preferences
- [ ] Pievienot vairāk kustību patternus
- [ ] Implementēt kustību mākslas (motion art) vizualizāciju
- [ ] Pievienot custom skaņu ielādes iespēju
- [ ] Multi-language atbalsts

## 📄 Licenza

MIT License - Brīvi izmantojams personāliem un komercprojektiem.

## 👨‍💻 Autors

Izveidots kā jautra eksperimentāla Android aplikācija sensoru un audio integrācijai.

---

**Prieka pilnu kustināšanu!** 🎉📱🔊
