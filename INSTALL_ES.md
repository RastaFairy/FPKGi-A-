# Guía de instalación y configuración

## Instalar el APK

### Requisitos previos

- Dispositivo Android con **Android 10 (API 29)** o superior
- Permitir la instalación desde fuentes desconocidas (aviso único)

### Pasos

1. Descarga el último `FPKGi-Manager-X.X.X.apk` desde la [página de Releases](https://github.com/RastaFairy/FPKGi-A-/releases/latest).
2. Abre el archivo descargado en tu dispositivo Android.
3. Si aparece el aviso *"Instalar apps desconocidas"*, pulsa **Ajustes → Permitir desde esta fuente**, vuelve atrás y pulsa **Instalar**.
4. La app aparecerá en tu lanzador como **FPKGi Manager**.

---

## Cargar tu librería de juegos

La app lee ficheros JSON en dos formatos:

### Formato FPKGi (dict)

```json
{
  "DATA": {
    "CUSA07995": {
      "title_id": "CUSA07995",
      "name": "A Way Out",
      "version": "01.01",
      "region": "USA",
      "min_fw": "9.00",
      "size": "16.21 GB",
      "pkg_url": "https://ejemplo.com/CUSA07995.pkg",
      "cover_url": "https://ejemplo.com/CUSA07995.jpg"
    }
  }
}
```

### Formato PS4PKGInstaller (lista)

```json
{
  "packages": [
    {
      "title_id": "CUSA07995",
      "name": "A Way Out",
      "version": "1.01",
      "region": "USA",
      "min_fw": "9.00",
      "size": "16.21 GB",
      "pkg_url": "https://ejemplo.com/CUSA07995.pkg"
    }
  ]
}
```

**Para cargar:** pulsa el icono de carpeta en la barra superior y selecciona tu fichero `.json`. La app también abre ficheros `.json` directamente desde el gestor de archivos del sistema.

---

## Configurar FTP (transferencia directa a PS4)

### En tu PS4

Activa un servidor FTP. Opciones habituales:

| Homebrew | Puerto por defecto |
|---|---|
| GoldHEN ftpd integrado | 2121 |
| PS4FTP (de Aldo Vargas) | 2121 |
| ftpd (independiente) | 21 |

Anota la **dirección IP** que muestra el servidor FTP (por ejemplo `192.168.1.210`).

### En FPKGi Manager

1. Ve a **Ajustes → Configuración FTP**.
2. Rellena:
   - **Host:** dirección IP de tu PS4
   - **Puerto:** 2121 (o el que use tu servidor)
   - **Ruta remota:** `/data/pkg` (o la carpeta que prefieras en la PS4)
   - **Modo pasivo:** activado (recomendado para redes domésticas)
3. Pulsa **Probar conexión** — deberías ver ✅ *Conexión OK*.
4. Activa el interruptor **Habilitar FTP**.

A partir de ahora, todas las descargas omiten el almacenamiento local y van directamente a tu PS4.

---

## Integración con OrbisPatches

No requiere configuración. Cuando abres la pantalla de detalle de un juego, la app consulta automáticamente [orbispatches.com](https://orbispatches.com) para obtener los parches disponibles.

- **Nivel 1 (rápido):** API JSON — instantáneo si la API está disponible.
- **Nivel 2 (fallback):** WebView Chromium integrado que renderiza la página y extrae los datos del DOM, sorteando Cloudflare. Tarda entre 1 y 3 segundos.
- **Caché de sesión:** una vez cargados los parches de un juego, se guardan en caché el resto de la sesión. Volver al mismo juego es instantáneo.

---

## Auto-actualización

En cada arranque, la app comprueba silenciosamente si hay una versión más reciente en GitHub. Si la hay, aparece un diálogo con el changelog. Pulsa **"Ver en GitHub"** para descargar e instalar el nuevo APK sin salir de la app.

Para desactivarla: actualmente no hay ajuste para deshabilitar la comprobación de actualizaciones. Realiza una única petición HTTPS de solo lectura a `api.github.com` y no hace nada más si no encuentra actualización.
