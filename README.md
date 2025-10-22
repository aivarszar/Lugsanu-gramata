# Convocatis Android App

Convocatis ir Android aplikācija dziesmu un lūgšanas tekstu lasīšanai.

## Galvenā funkcionalitāte

### ✅ Implementētas funkcijas:

#### 1. **Tekstu lasīšana un pārvaldība**
- **TextsFragment** - tekstu saraksts ar RecyclerView
- **TextReadingFragment** - teksta satura lasīšana
- Meklēšanas funkcionalitāte
- Šķirošanas un filtrēšanas opcijas

#### 2. **Autentifikācija**
- **LoginFragment** - vienkārša email autentifikācija
- Profila saglabāšana SharedPreferences
- Logout funkcionalitāte

#### 3. **Profila pārvaldība**
- **ProfileFragment** - lietotāja profila skatīšana
- ProfileEntity datu modelis

#### 4. **Datu bāze (Room)**
- **TextEntity** - dziesmu/lūgšanu teksti
- **LanguageEntity** - valodas
- **DenominationEntity** - konfesijas
- **TextUsageEntity** - tekstu izmantošanas statistika
- **ProfileEntity** - lietotāja profils (SharedPreferences)

#### 5. **UI komponenti**
- MainActivity ar Navigation Drawer
- Material Design Components
- Fragment-based navigācija
- Action Bar ar meklēšanu

### 🔧 Tehniskais steks:

- **Valoda**: Kotlin
- **Datu bāze**: Room
- **UI**: Material Design Components, ViewBinding
- **Arhitektūra**: MVVM gatavībā (ViewModel, LiveData)
- **Async operācijas**: Kotlin Coroutines
- **Networking**: Retrofit + OkHttp (gatavs API implementācijai)
- **Dependencies**: AndroidX, Navigation Component

### 📋 Nākamie soļi:

1. **API integrācija** - Retrofit serviss ar backend API
2. **Sinhronizācijas serviss** - Datu sinhronizācija ar serveri
3. **Pilnīga tekstu pārvaldība** - Highlighting, editing, sharing
4. **ViewPager navigācija** - Swipe navigācija starp tekstiem
5. **Like/Rating sistēma** - Tekstu vērtēšana
6. **Sociālo tīklu login** - Facebook/Google autentifikācija

## Projekta struktūra:

```
app/src/main/java/com/convocatis/app/
├── database/
│   ├── entity/          # Room entities
│   ├── dao/             # Data Access Objects
│   └── AppDatabase.kt   # Room database
├── ui/
│   └── fragments/       # UI fragments
│       ├── TextsFragment.kt
│       ├── TextReadingFragment.kt
│       ├── LoginFragment.kt
│       └── ProfileFragment.kt
├── network/             # API servisi (gatavs)
├── repository/          # Datu repozitoriji (gatavs)
├── viewmodel/           # ViewModels (gatavs)
├── utils/               # Utility klases (gatavs)
├── ConvocatisApplication.kt  # Application class
└── MainActivity.kt      # Main activity
```

## Piezīmes:

- Grupas, paziņojumi un lūgšanas atgādinājumi ir izņemti (plānots veidot ko jaunu nākotnē)
- Reklāmas pagaidām nav iekļautas
- Fokuss uz galveno funkcionalitāti - tekstu lasīšanu
