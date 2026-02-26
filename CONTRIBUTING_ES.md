# Cómo contribuir a FPKGi Manager Android

Gracias por considerar contribuir. Las siguientes pautas mantienen el proceso limpio y predecible.

---

## Índice

- [Reportar errores](#reportar-errores)
- [Sugerir funciones](#sugerir-funciones)
- [Configurar el entorno](#configurar-el-entorno)
- [Proceso de pull request](#proceso-de-pull-request)
- [Estilo de código](#estilo-de-código)
- [Política de versiones](#política-de-versiones)
- [Mensajes de commit](#mensajes-de-commit)

---

## Reportar errores

Antes de abrir un issue:

1. Busca en los [issues existentes](https://github.com/RastaFairy/FPKGi-A-/issues) para evitar duplicados.
2. Reproduce el error en la última versión publicada.

Al abrir el issue, incluye:

- Versión de la app (visible en Ajustes)
- Versión de Android y modelo del dispositivo
- Pasos para reproducir el error
- Comportamiento esperado frente al comportamiento real
- Salida de Logcat si está disponible (`adb logcat | grep fpkgi`)

---

## Sugerir funciones

Abre un Issue en GitHub con la etiqueta `enhancement`. Describe:

- El problema que resuelve
- El comportamiento propuesto
- Ejemplos relevantes de la app Python de referencia (`fpkgi_manager_with_ftp.py`) si los hubiera

---

## Configurar el entorno

```bash
# 1. Haz fork y clona el repositorio
git clone https://github.com/TU_USUARIO/FPKGi-A-.git
cd FPKGi-A-

# 2. Ábrelo en Android Studio (Hedgehog o posterior)
# 3. Sincroniza Gradle — todas las dependencias se descargan automáticamente

# 4. Compila el APK debug
./gradlew assembleDebug
# Salida: app/build/outputs/apk/debug/app-debug.apk
```

**Requisitos:**

| Herramienta | Versión mínima |
|---|---|
| Android Studio | Hedgehog (2023.1.1) |
| JDK | 21 |
| Android SDK | API 36 |
| Kotlin | 2.1.0 |
| Gradle | 8.14 |

---

## Proceso de pull request

1. **Nombre de rama:** `feature/descripcion-corta`, `fix/descripcion-corta`, `chore/descripcion-corta`
2. Un único cambio lógico por PR — no mezcles funciones y correcciones.
3. Actualiza `CHANGELOG.md` y `CHANGELOG_ES.md` bajo una sección `[Sin publicar]`.
4. Actualiza `README.md` / `README_ES.md` si el cambio afecta al comportamiento documentado.
5. Actualiza `versionName` y `versionCode` en `app/build.gradle.kts` siguiendo la [política de versiones](#política-de-versiones).
6. Todo el código existente debe compilar sin errores (`./gradlew assembleDebug`).
7. La descripción del PR debe referenciar el issue relacionado (`Fixes #123`).

---

## Estilo de código

- **Lenguaje:** solo Kotlin — sin Java.
- **Formato:** sigue el estilo del fichero existente; indentación de 4 espacios, sin espacios en blanco al final.
- **Imports:** sin imports con wildcard salvo en Compose (`import androidx.compose.material3.*` es aceptable).
- **Corrutinas:** todas las llamadas de red deben ejecutarse en `Dispatchers.IO`; todas las actualizaciones de estado de UI en `Dispatchers.Main`.
- **Sin cuerpo de expresión + return bare:** Kotlin prohíbe `return` dentro de cuerpos de expresión `= expresión`. Usa siempre cuerpos de bloque `{ ... }` cuando la función contiene sentencias `return`.
- **Imports duplicados:** cada import debe aparecer exactamente una vez.
- **i18n:** cualquier cadena visible por el usuario debe añadirse a `StringResources.kt` en los **6 idiomas** (español, inglés, alemán, francés, italiano, japonés). No uses cadenas hardcodeadas en los composables.

---

## Política de versiones

| Alcance del cambio | Incremento |
|---|---|
| Corrección de error u optimización menor | `+0.0.1` |
| Nueva función o mejora significativa | `+0.1` |
| Bloque de funciones importantes o rediseño | `+1.0` |

`versionCode` refleja los dígitos de `versionName`: versión `6.5.2` → `versionCode = 652`.

---

## Mensajes de commit

Sigue [Conventional Commits](https://www.conventionalcommits.org/es/):

```
feat: añadir pull-to-refresh en la sección de OrbisPatches
fix: eliminar import duplicado de Uri en MainViewModel
chore: actualizar versión a 6.5.3
docs: actualizar CHANGELOG para 6.5.2
```

Tipos: `feat`, `fix`, `chore`, `docs`, `refactor`, `perf`, `test`.
