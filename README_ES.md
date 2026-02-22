# 🎮 FPKGi Manager — Android

<p align="center">
  <img src="https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-1.7-4285F4?logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/licencia-MIT-blue" />
  <img src="https://img.shields.io/badge/versión-6.0-00D4FF" />
</p>

<p align="center">
  Gestor nativo para Android de bibliotecas PS4 en formato Fake-PKG.<br>
  Navega tu catálogo, consulta el historial de parches en OrbisPatches<br>
  y envía PKGs directamente a tu PS4 por Wi-Fi sin necesidad de PC.
</p>

> 🖥️ ¿Buscas la **app de escritorio (Python)**? → [FPKGi-for-PY](https://github.com/RastaFairy/FPKGi-for-PY)

---

## ✨ Características

| Función | Descripción |
|---------|-------------|
| 📂 **Doble formato JSON** | Formato dict de FPKGi + formato list de PS4PKGInstaller |
| 📡 **Transferencia FTP** | Envía PKGs directamente a la PS4 por Wi-Fi local — sin necesidad de PC |
| ⬇️ **Descarga local** | Guarda PKGs en el almacenamiento del dispositivo |
| ✅ **Verificación de disponibilidad** | Comprobación HTTP HEAD por juego — estado verde/rojo en tiempo real |
| 🔍 **OrbisPatches** | Consulta el historial de parches y notas de versión por título |
| 🌍 **UI multiidioma** | Español e inglés incluidos |
| 🎨 **Tema oscuro PS4** | Diseño Material 3 inspirado en la interfaz XMB de PS4 |
| ⚙️ **Ajustes persistentes** | Host FTP, puerto, credenciales y ruta remota guardados con DataStore |

---

## 📋 Requisitos

### Dispositivo
- Android 10+ (API 29) — compatible con cualquier dispositivo Android desde 2020

### Para transferencia FTP
- PS4 con servidor FTP activo:
  - **GoldHEN FTP Server** *(recomendado)*
  - PS4FTP Homebrew
  - Cualquier servidor FTP en el puerto 2121
- PS4 y dispositivo Android en la **misma red Wi-Fi**

### Compilación
- Android Studio Hedgehog (2023.1) o superior
- JDK 17
- Android SDK: API 29 (mínimo) – API 35 (compilación)
- Kotlin 2.0
- Jetpack Compose 1.7

---

## 🚀 Compilar e instalar

### Desde Android Studio

1. Clona el repositorio:

```bash
git clone https://github.com/RastaFairy/FPKGi-A-.git
cd FPKGi-A-
```

2. Abre el proyecto en Android Studio.
3. Sincroniza Gradle y pulsa **Run → Run 'app'**.

### Desde la línea de comandos

```bash
./gradlew assembleDebug
# APK → app/build/outputs/apk/debug/app-debug.apk
```

### Build de release

```bash
./gradlew assembleRelease
# Firma con tu keystore antes de distribuir
```

---

## 📡 Configuración FTP

Abre **Ajustes** en la app y rellena:

| Campo | Valor por defecto | Descripción |
|-------|------------------|-------------|
| Host | `192.168.1.XXX` | IP local de tu PS4 |
| Puerto | `2121` | Puerto del servidor FTP |
| Usuario | `anonymous` | Déjalo vacío para GoldHEN |
| Contraseña | *(vacío)* | Déjalo vacío para GoldHEN |
| Ruta remota | `/data/pkg` | Carpeta de destino en la PS4 |
| Modo pasivo | ✓ activado | Necesario en la mayoría de redes domésticas |

---

## 📁 Estructura del proyecto

```
FPKGi-A-/
├── app/
│   └── src/main/
│       ├── java/com/fpkgi/manager/
│       │   ├── MainActivity.kt          # Punto de entrada
│       │   ├── MainViewModel.kt         # Estado MVVM + lógica de negocio
│       │   ├── data/
│       │   │   ├── model/Models.kt      # Data classes (Game, Patch, Config)
│       │   │   └── repository/
│       │   │       └── SettingsRepository.kt   # Persistencia con DataStore
│       │   ├── i18n/
│       │   │   └── StringResources.kt   # Sistema multiidioma
│       │   ├── network/
│       │   │   ├── FtpDownloadService.kt  # Servicio en primer plano para FTP
│       │   │   └── OrbisClient.kt         # Cliente HTTP para OrbisPatches
│       │   └── ui/
│       │       ├── components/
│       │       │   └── Components.kt    # Componentes Compose reutilizables
│       │       ├── screens/
│       │       │   ├── GameListScreen.kt
│       │       │   ├── GameDetailScreen.kt
│       │       │   ├── DownloadsScreen.kt
│       │       │   └── SettingsScreen.kt
│       │       └── theme/Theme.kt       # Tema oscuro Material 3 estilo PS4
│       └── utils/
│           └── JsonParser.kt            # Parser de doble formato JSON
├── gradle/libs.versions.toml            # Catálogo de versiones
└── .github/
    ├── workflows/
    │   ├── ci.yml                       # APK de debug en cada push a main
    │   └── release.yml                  # APK de release al crear un tag v*
    └── ISSUE_TEMPLATE/
        ├── bug_report.yml
        └── feature_request.yml
```

---

## 🏗️ Arquitectura

La app sigue el patrón **MVVM** con flujo de datos unidireccional:

```
Pantallas UI (Compose)
        ↕
MainViewModel (StateFlow)
        ↕
Repositorios / Clientes de red
        ↕
DataStore / FTP / HTTP
```

- **`MainViewModel`** — fuente única de verdad para la lista de juegos, estados de descarga y datos de parches
- **`FtpDownloadService`** — servicio en primer plano de Android para que las transferencias sobrevivan al apagar la pantalla
- **`OrbisClient`** — cliente HTTP basado en corrutinas para OrbisPatches
- **`JsonParser`** — parsea ambos formatos de catálogo JSON en un modelo `Game` unificado

---

## 🤝 Contribuir

1. Haz un fork del repositorio y crea una rama para tu función.
2. Asegúrate de que el proyecto compila con `./gradlew assembleDebug`.
3. Abre un Pull Request hacia `main`.

Usa las plantillas de issues **Bug Report** o **Feature Request**.

---

## 📜 Créditos

| Rol | Nombre |
|-----|--------|
| Concepto original (PSP Homebrew) | [Bucanero](https://github.com/bucanero) |
| Puerto PS4 / PS5 | ItsJokerZz |
| Edición Python | RastaFairy |
| Puerto Android | RastaFairy |

---

## 📄 Licencia

[MIT](LICENSE) © RastaFairy
