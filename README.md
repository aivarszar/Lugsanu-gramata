# Convocatis Android App

Convocatis ir Android aplikācija dziesmu un lūgšanas tekstu lasīšanai ar funkcionalitāti tekstu pārvaldībai, grupām, paziņojumiem un sinhronizāciju.

## Migrētā funkcionalitāte no vecā projekta

### ✅ Implementētas funkcijas:

#### 1. **Datu bāzes struktūra (Room Database)**
- TextEntity - dziesmu/lūgšanu teksti
- ProfileEntity - lietotāja profils
- GroupEntity - grupas
- NotificationEntity - paziņojumi
- LanguageEntity - valodas
- DenominationEntity - konfesijas
- TextUsageEntity - tekstu izmantošanas statistika

#### 2. **Galvenā UI struktūra**
- MainActivity ar Navigation Drawer
- Custom tab menu (Texts/Notifications/Groups)
- Fragment-based navigācija

#### 3. **Tekstu lasīšanas funkcionalitāte**
- TextsFragment - tekstu saraksts
- TextReadingFragment - teksta satura lasīšana
- RecyclerView ar adapteriem

#### 4. **Autentifikācija**
- LoginFragment - vienkārša email autentifikācija
- Profila saglabāšana SharedPreferences
- Logout funkcionalitāte

#### 5. **Profila pārvaldība**
- ProfileFragment - lietotāja profila skatīšana
- ProfileEntity datu modelis ar visiem vecā projekta laukiem

#### 6. **Navigācija un izvēlnes**
- Navigation Drawer ar izvēlni
- Action Bar ar meklēšanas funkcionalitāti
- Custom tab menu tekstu/paziņojumu/grupu pārvaldībai

#### 7. **Grupas un paziņojumi**
- GroupsFragment (placeholder)
- NotificationsFragment (placeholder)
- Datu bāzes modeļi gatavi pilnai implementācijai

### 🔧 Tehniskais steks:

- **Valoda**: Kotlin
- **Datu bāze**: Room (migrēts no ORMLite)
- **UI**: Material Design Components, ViewBinding
- **Arhitektūra**: MVVM gatavībā (ViewModel, LiveData)
- **Async operācijas**: Kotlin Coroutines
- **Networking**: Retrofit + OkHttp (gatavs API implementācijai)
- **Dependencies**: AndroidX, Navigation Component, WorkManager

### 📋 Nākamie soļi (gatavi implementācijai):

1. **API integrācija** - Retrofit serviss ar convocatis.net API
2. **Sinhronizācijas serviss** - Datu sinhronizācija ar serveri
3. **Alarm/Atgādinājumi** - WorkManager implementācija lūgšanas atgādinājumiem
4. **Pilnīga tekstu pārvaldība** - Filtrēšana, šķirošana, meklēšana
5. **ViewPager navigācija** - Tekstuālais pārslēgšanās starp tekstiem
6. **Like/Rating sistēma** - Tekstu vērtēšana
7. **Grupu funkcionalitāte** - Pilnīga grupu pārvaldība
8. **Paziņojumu sistēma** - Push notifications un paziņojumu pārvaldība
9. **Facebook/Google login** - Sociālo tīklu autentifikācija
10. **AdMob integrācija** - Reklāmu pievienošana

## Projekta struktūra:

```
app/src/main/java/com/convocatis/app/
├── database/
│   ├── entity/          # Room entities
│   ├── dao/             # Data Access Objects
│   └── AppDatabase.kt   # Room database
├── ui/
│   └── fragments/       # UI fragments
├── network/             # API servisi (gatavs)
├── repository/          # Datu repozitoriji (gatavs)
├── viewmodel/           # ViewModels (gatavs)
├── workers/             # WorkManager workers (gatavs)
├── utils/               # Utility klases (gatavs)
├── ConvocatisApplication.kt  # Application class
└── MainActivity.kt      # Main activity

