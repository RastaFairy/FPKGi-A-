# Historial de cambios

Todos los cambios relevantes de FPKGi Manager Android están documentados aquí.  
El formato sigue [Keep a Changelog](https://keepachangelog.com/es/1.0.0/).  
El versionado sigue `MAYOR.MENOR.PARCHE` — consulta la [política de versiones](#política-de-versiones).

---

## [6.5.2] — 2026-02-26

### Añadido
- **Instalación in-app de APKs** — cuando se detecta una versión nueva, la app descarga el `.apk` al caché interno y lanza el instalador del sistema por encima de la pantalla actual. Sin necesidad de navegador, sin salir de la app.
- Declaración de `FileProvider` en `AndroidManifest.xml` para servir el APK descargado al instalador del sistema de forma segura.
- Permiso `REQUEST_INSTALL_PACKAGES`.
- Barra de progreso de descarga dentro del diálogo de actualización (`LinearProgressIndicator`, 0–100%).
- Estado de error en el diálogo de actualización si la descarga falla.
- Botón "Actualizar" deshabilitado durante la descarga para evitar pulsaciones dobles.
- Fallback al navegador si la release de GitHub no tiene un asset `.apk` adjunto.

### Cambiado
- El diálogo de actualización ahora distingue tres estados: *inactivo*, *descargando*, *error*.
- El botón "Ver en GitHub" desencadena la descarga in-app en lugar de abrir el navegador (cuando existe un asset APK).
- `StringResources`: añadidos `updateDownloading` y `updateError` en los 6 idiomas.

### Corregido
- `android.net.Uri` y `java.io.File` importados dos veces en `MainViewModel.kt`.

---

## [6.5.1] — 2026-02-26

### Añadido
- **Polling DOM** en `OrbisWebViewClient` — en lugar de una espera fija de 3.500 ms, el script se evalúa cada 400 ms desde que se dispara `onPageFinished`. Se detiene en cuanto aparecen elementos `.patch-wrapper`. El tiempo de carga habitual baja de ~4–5 s a ~1–2 s.
- **Caché de sesión** en `OrbisClient` — `sessionCache: Map<String, OrbisResult>` guarda los resultados en memoria durante la vida del proceso. Volver al mismo juego es instantáneo. Se expone `invalidateCache()` para soporte futuro de pull-to-refresh.

### Cambiado
- `OrbisWebViewClient`: espera inicial tras `onPageFinished` reducida de 3.500 ms a 600 ms.
- Máximo de intentos de polling fijado en 25 (techo de 10 s).

---

## [6.5] — 2026-02-26

### Añadido
- **Comprobador de actualizaciones** (`UpdateChecker.kt`) — consulta `https://api.github.com/repos/RastaFairy/FPKGi-A-/releases/latest` al arrancar. Compara segmentos de versión numéricamente (no lexicográficamente), por lo que `6.10 > 6.9` funciona correctamente. Los sufijos de texto (`-ftp`, `-beta`) se ignoran en la comparación.
- Diálogo de actualización con changelog del release (hasta 600 caracteres).
- `StateFlow`s `updateInfo` y `updateProgress` en `MainViewModel`.
- `checkForAppUpdate()` y `dismissUpdate()` en `MainViewModel`.
- Cadenas de actualización en los 6 idiomas: `updateTitle`, `updateMessage`, `updateChangelog`, `updateConfirm`, `updateLater`.

### Cambiado
- `versionCode` actualizado a `650`, `versionName` a `"6.5"`.

---

## [6.4.3] — 2026-02-26

### Corregido
- OrbisPatches mostraba listas de parches idénticas para juegos diferentes (p.ej. tanto *A Hat in Time* como *A Monster's Expedition* mostraban la misma lista v05.05, v12.19…).
  - **Causa raíz 1:** `OrbisWebViewClient` no limpiaba la caché del WebView entre peticiones. Corregido con `clearCache(true)`, `clearHistory()`, `WebStorage.deleteAllData()` y `cacheMode = LOAD_NO_CACHE`.
  - **Causa raíz 2:** El script JS de extracción leía `window.__NUXT__` globalmente, que contiene versiones de dependencias npm/webpack (p.ej. `"tailwindcss": "14.09"`) que coincidían con el regex de versiones. Corregido usando selectores DOM puros.
  - **Causa raíz 3:** El regex `\d{2}\.\d{2}` capturaba cualquier cadena de versión de dos dígitos. Añadido validador `isValidPs4Version()` que restringe el componente mayor a `01–20`.
- `OrbisResult` del juego anterior persistía al navegar a uno nuevo. Corregido con `DisposableEffect { onDispose { viewModel.clearOrbisResult() } }` en `GameDetailScreen`.

---

## [6.4.2] — 2026-02-26

### Cambiado
- Eliminado el botón **"Descargar este parche"** de cada `PatchCard`. La sección es meramente informativa — la descarga de PKGs de parches individuales estaba fuera del alcance.
- Parámetro `onDownload` de `PatchCard` eliminado por completo; cadena `btnDownloadPatch` reemplazada por `patchNoNotes` en los 6 idiomas.
- El card del parche más reciente ahora **se expande por defecto**.
- El indicador de compatibilidad de FW muestra el prefijo emoji 🟢 / 🔴 igual que en la app Python de referencia.
- Se muestra "Sin notas de parche" en cursiva cuando `notes` está vacío (antes no se mostraba nada).
- La comparación del badge "tu FPKG" es más robusta: elimina ceros iniciales y prefijos `v` antes de comparar.

---

## [6.4.1] — 2026-02-26

### Corregido
- `OrbisClient.kt` línea 178: `return null` dentro de un cuerpo de expresión (`= try { ... }`). Kotlin prohíbe `return` bare en cuerpos de expresión. Las tres funciones afectadas (`tryJsonApi`, `tryHttpPage`, `httpGet`) convertidas a cuerpos de bloque.
- `LocalPkgBrowserScreen.kt` líneas 388 y 473: referencia al color `NavyLight` no resuelta. Reemplazada por `NavyMid`.
- Función `parseDomPatches`: mismo patrón de cuerpo de expresión + `return` corregido.

---

## [6.4] — 2026-02-24

### Añadido
- **Nivel WebView de OrbisPatches** (`OrbisWebViewClient.kt`): el WebView de Android renderiza la página completa (Chromium, sortea Cloudflare) y un script JS extrae los datos de parches usando exactamente los mismos selectores CSS que el parser Python de referencia (`_parse_orbis_html`):
  - `.patch-wrapper` — contenedor de cada parche
  - `a.patch-link[data-contentver]` — versión
  - `.patch-container.latest` — badge de última versión
  - `.col-auto.text-end` [0, 1, 2] — tamaño, FW requerido, fecha de creación
  - `a.changeinfo-preview[data-patchnotes-charcount > 0]` — notas del parche
- Caché limpiada antes de cada petición (`clearCache`, `clearHistory`, `LOAD_NO_CACHE`) para garantizar datos frescos.
- **Explorador de PKGs locales** (`LocalPkgBrowserScreen.kt`): escanea `Downloads/`, directorios externos de la app, `PKG/`, `PS4/` y `FPKGI/`. Muestra nombre, tamaño legible y fecha de modificación. Botón de subida FTP por fichero.
- Clase de datos `LocalPkgFile` con `humanSize` y `humanDate` calculados.
- 9 nuevas cadenas i18n para el explorador de PKGs en los 6 idiomas.
- Método `uploadLocalPkg()` en `MainViewModel`.

### Cambiado
- Pipeline de `OrbisClient` simplificado a 2 niveles: API JSON (ruta rápida) → DOM WebView.
- Nivel HTTP-only eliminado (Cloudflare lo bloqueaba de forma fiable; WebView lo sustituye por completo).
- `SettingsScreen`: "Enviar PKG local" reemplazado por botón de navegación al nuevo explorador de PKGs.

---

## [6.3] — 2026-02-23

### Añadido
- **i18n en 6 idiomas** (`StringResources.kt`): español, inglés, alemán, francés, italiano y japonés. Selección de idioma persistida en `DataStore`.
- **Pausa/reanudación de descargas** en `FtpDownloadService`.
- **Subida FTP** de ficheros `.pkg` almacenados localmente a la PS4.
- Gestión de caché de iconos en Ajustes (limpiar caché de disco + memoria, mostrar tamaño).
- `OrbisClient` nivel 1: API JSON en `orbispatches.com/api/patch.php`.
- `OrbisClient` nivel 2: scraping de página HTTP con headers Chrome completos.

### Cambiado
- `MainViewModel` ahora pasa el contexto `Application` a `OrbisClient` para el soporte de WebView.
- Navegación actualizada: ruta `pkgbrowser` añadida a `FPKGiNavHost`.

---

## [6.2] — 2026-02-23

### Añadido
- Servicio en primer plano `FtpDownloadService` para descargas en segundo plano y transferencia FTP directa a la PS4.
- Pantalla de configuración FTP (host, puerto, usuario, contraseña, ruta remota, modo pasivo, timeout, probar conexión).
- `DownloadsScreen` con progreso por elemento, pausa, reanudación y cancelación.
- Modelos de datos `DownloadItem` y `FtpConfig`.
- `SettingsRepository` usando `DataStore` para preferencias persistentes.

---

## [6.1] — 2026-02-21

### Añadido
- `GameDetailScreen`: verificación de disponibilidad, botón de descarga de PKG, sección de OrbisPatches con `PatchCard` expandible por versión.
- `PatchCard`: versión, chip de color de compatibilidad FW, tamaño, fecha de creación, sección de notas expandible.
- `OrbisClient`: scraper HTTP básico con suplantación de `User-Agent`.
- Modelos de datos `OrbisResult` / `OrbisPatch`.
- Estado de disponibilidad (`CHECKING`, `AVAILABLE`, `UNAVAILABLE`) en el modelo `Game`.
- Carga de iconos basada en Coil desde miniaturas de URL de PKG con caché en disco.

### Corregido
- Padre del tema Material3 que causaba fallo de enlace de recursos AAPT.
- Errores de inferencia de tipos en Kotlin 2.1.0 para `OrbisClient`.
- Import de `LocalUriHandler` faltante en `GameDetailScreen`.

---

## [6.0] — 2026-02-21

### Añadido
- Puerto Android inicial de FPKGi Manager (anteriormente app de escritorio en Python/Tkinter).
- `GameListScreen` con búsqueda, ordenación multi-columna y contador de juegos.
- Soporte de doble formato JSON: FPKGi dict (clave `DATA`) y lista plana PS4PKGInstaller.
- `JsonParser` con detección automática de formato y formateo legible de tamaños.
- `FPKGiTheme` oscuro navy/cyan/dorado (Jetpack Compose Material3).
- Navegación `NavHost` con animaciones de deslizamiento.
- Soporte de pantalla completa (edge-to-edge).
- Filtro de intención `ACTION_VIEW` para abrir ficheros `.json` directamente desde el gestor de archivos.

---

## Política de versiones

| Alcance del cambio | Incremento |
|---|---|
| Corrección de error, optimización menor | `+0.0.1` |
| Nueva función o mejora significativa | `+0.1` |
| Bloque de funciones importantes o rediseño | `+1.0` |
