# 🎮 FPKGi Manager — Android

<p align="center">
  <img src="https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-1.7-4285F4?logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/License-MIT-blue" />
  <img src="https://img.shields.io/badge/version-5.11--FTP-00D4FF" />
</p>

<p align="center">
  Gestor de FPKGs para PS4 con soporte de <strong>descarga directa via FTP</strong>,<br>
  integración con <strong>OrbisPatches</strong> y soporte de múltiples formatos de catálogo.
</p>

---

## ✨ Características

| Función | Descripción |
|---------|-------------|
| 📂 **Carga de catálogos** | Soporta formato FPKGi dict y PS4PKGInstaller list |
| 📡 **Descarga FTP** | Envía PKGs directamente a tu PS4 sin pasar por el PC |
| ⬇️ **Descarga local** | Guarda PKGs en el almacenamiento del dispositivo |
| 🔍 **Verificación** | Comprueba disponibilidad de PKGs via HTTP HEAD |
| 🗂️ **OrbisPatches** | Consulta parches y actualizaciones disponibles |
| 🌍 **Multiidioma** | UI en español, inglés y más |
| 🎨 **Tema oscuro** | Diseño inspirado en la interfaz de PS4 |

---

## 📋 Requisitos

### Android
- Android 10+ (API 29) — compatible con todos los dispositivos Android desde 2020
- Kotlin 2.0 / Jetpack Compose

### Para descarga FTP
- PS4 con servidor FTP activo:
  - **GoldHEN FTP Server** (recomendado)
  - PS4FTP Homebrew
  - Cualquier servidor FTP en puerto 2121
- PS4 y Android en la **misma red WiFi**

---

## 🚀 Compilar el proyecto

### Prerrequisitos
- Android Studio Hedgehog+ o superior
- JDK 17
- Android SDK (API 29–35)

### Pasos

```bash
git clone https://github.com/TU_USUARIO/FPKGi-Android.git
cd FPKGi-Android
./gradlew assembleDebug
# APK en: app/build/outputs/apk/debug/
```

### Compilar release

```bash
./gradlew assembleRelease
# Firma el APK con tu keystore
```

---

## ⚙️ Configuración FTP

1. **Abre la app** → toca ⚙️ *Ajustes*
2. **Habilita FTP** con el toggle
3. **Rellena los campos**:
   - IP de tu PS4 (Ajustes → Red → Ver estado de conexión)
   - Puerto: `2121`
   - Usuario: `anonymous`
   - Directorio: `/data/pkg`
4. **Prueba la conexión** → debe aparecer ✅
5. **Guarda** y empieza a descargar 🎮

---

## 📦 Formatos de catálogo soportados

### FPKGi dict
```json
{
  "DATA": {
    "https://example.com/game.pkg": {
      "title_id": "CUSA00001",
      "name": "Mi Juego",
      "version": "01.05",
      "region": "EUR",
      "size": 5368709120,
      "min_fw": "9.00",
      "cover_url": "https://example.com/cover.jpg"
    }
  }
}
```

### PS4PKGInstaller list
```json
{
  "packages": [
    {
      "title_id": "CUSA00001",
      "name": "Mi Juego",
      "version": "01.05",
      "region": "EUR",
      "size": "5.0 GB",
      "system_version": "9.00",
      "icon_url": "https://example.com/cover.jpg",
      "pkg_url": "https://example.com/game.pkg"
    }
  ]
}
```

---

## 🏗️ Arquitectura

```
FPKGi-Android/
├── app/src/main/java/com/fpkgi/manager/
│   ├── MainActivity.kt               # Entry point + navegación
│   ├── MainViewModel.kt              # ViewModel principal
│   ├── data/
│   │   ├── model/Models.kt           # Modelos de datos
│   │   └── repository/SettingsRepository.kt  # DataStore
│   ├── network/
│   │   ├── FtpDownloadService.kt     # Servicio FTP foreground
│   │   └── OrbisClient.kt           # Cliente OrbisPatches
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── GameListScreen.kt     # Pantalla principal
│   │   │   ├── GameDetailScreen.kt   # Detalle + parches
│   │   │   ├── DownloadsScreen.kt    # Gestor descargas
│   │   │   └── SettingsScreen.kt    # Config FTP
│   │   ├── components/Components.kt  # Componentes reutilizables
│   │   └── theme/Theme.kt           # Tema oscuro PS4
│   └── utils/JsonParser.kt          # Parser de catálogos
└── .github/
    ├── workflows/
    │   ├── ci.yml                    # CI: build en cada push
    │   └── release.yml              # Release: APK firmado en tag
    └── ISSUE_TEMPLATE/             # Templates de issues
```

**Stack tecnológico:**
- **Kotlin** — lenguaje principal
- **Jetpack Compose** — UI declarativa moderna
- **Material 3** — design system
- **Coroutines + Flow** — concurrencia reactiva
- **DataStore** — persistencia de configuración
- **Apache Commons Net** — cliente FTP robusto
- **Coil** — carga de imágenes asíncrona
- **WorkManager** — descargas en segundo plano

---

## 📡 CI/CD

El proyecto incluye GitHub Actions configuradas:

- **CI** (`ci.yml`): Se ejecuta en cada push/PR a `main` y `develop`. Compila el APK debug.
- **Release** (`release.yml`): Se ejecuta al crear un tag `v*.*`. Genera y publica el APK firmado.

### Secrets necesarios para Release

| Secret | Descripción |
|--------|-------------|
| `SIGNING_KEY_BASE64` | Keystore en base64 |
| `SIGNING_KEY_ALIAS` | Alias de la clave |
| `SIGNING_KEY_PASSWORD` | Contraseña de la clave |
| `SIGNING_STORE_PASSWORD` | Contraseña del keystore |

---

## 🤝 Contribuir

1. Fork el repositorio
2. Crea una rama: `git checkout -b feature/mi-funcion`
3. Haz tus cambios y commits
4. Push: `git push origin feature/mi-funcion`
5. Abre un Pull Request

---

## 📜 Créditos

- **Concepto original:** Bucanero (PSP Homebrew)
- **Puerto PS4/PS5:** ItsJokerZz
- **Python Edition:** RastaFairy
- **Android Port:** FPKGi Team

---

## ⚖️ Licencia

MIT License — consulta [LICENSE](LICENSE) para más detalles.

> ⚠️ **Aviso:** Esta aplicación es solo para uso con copias legítimas de software.
> Respetar los derechos de autor de los desarrolladores de videojuegos.
