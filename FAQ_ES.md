# Preguntas frecuentes

## General

**P: ¿Qué es FPKGi Manager?**  
R: Una app Android para explorar, verificar y descargar paquetes FPKG de PS4. También muestra información de parches de OrbisPatches y envía ficheros directamente a tu PS4 por FTP.

**P: ¿Funciona con paquetes de PS5?**  
R: No. La app está diseñada exclusivamente para ficheros FPKG de PS4.

**P: ¿Es seguro usarla?**  
R: La app solo lee ficheros JSON locales y realiza peticiones de red a las URLs ya presentes en tu JSON. No modifica ningún fichero del dispositivo fuera de su propia caché interna.

---

## Librería JSON

**P: ¿De dónde obtengo el fichero JSON?**  
R: Expórtalo desde el FPKGi Manager de escritorio o desde PS4PKGInstaller. El formato está documentado en [INSTALL_ES.md](INSTALL_ES.md).

**P: La app muestra 0 juegos después de cargar el JSON.**  
R: Comprueba que el JSON coincide con uno de los dos formatos compatibles. Abre el fichero en un editor de texto y verifica que tiene una clave `DATA` (formato FPKGi) o un array `packages` (formato PS4PKGInstaller).

**P: ¿Puedo abrir el JSON directamente desde mi gestor de archivos?**  
R: Sí. La app se registra como manejador de ficheros `.json`. Pulsa el fichero en cualquier gestor de archivos y elige FPKGi Manager.

---

## Descargas y FTP

**P: La descarga comienza pero no llega nada a la PS4.**  
R: Comprueba que FTP está habilitado en Ajustes, que la IP es correcta y que el servidor FTP de la PS4 está en marcha. Usa el botón **Probar conexión** para verificarlo antes de descargar.

**P: ¿Qué servidor FTP debo usar en la PS4?**  
R: Cualquier servidor FTP homebrew funciona. Los más habituales son el ftpd integrado de GoldHEN (puerto 2121) y el homebrew ftpd independiente.

**P: ¿Puedo pausar y reanudar una descarga?**  
R: Sí. Ve a la pantalla de Descargas y usa los botones de pausa/reanudación por elemento.

**P: ¿Dónde se guardan los ficheros si FTP está desactivado?**  
R: En la carpeta `Downloads/FPKGi/` de tu dispositivo, organizada por Title ID.

---

## OrbisPatches

**P: La lista de parches está vacía o muestra "sin resultados".**  
R: Es posible que el título no tenga parches listados en OrbisPatches, o que Cloudflare esté bloqueando la petición. Intenta pulsar el botón 🌐 para abrir la página en tu navegador y comprobarlo manualmente.

**P: Los parches tardan en cargarse la primera vez.**  
R: La primera carga usa el WebView Chromium integrado para renderizar la página y sortear Cloudflare. Esto tarda entre 1 y 3 segundos. Las visitas posteriores al mismo juego en la misma sesión son instantáneas (caché de sesión).

**P: Las versiones de los parches parecen incorrectas (p.ej. v45.73, v14.09).**  
R: Esto era un bug en versiones anteriores a la 6.4.3, donde se capturaban versiones de librerías JavaScript del estado Nuxt de la página en lugar de versiones reales de parches de PS4. Actualiza a la 6.4.3 o posterior.

---

## Actualizaciones

**P: ¿Cómo funciona la actualización in-app?**  
R: Al arrancar, la app consulta la API de GitHub Releases. Si existe una versión más reciente, muestra un diálogo con el changelog. Al pulsar "Ver en GitHub", se descarga el APK al caché interno de la app y se lanza el instalador del sistema por encima de la pantalla actual.

**P: He cerrado el diálogo de actualización. ¿Cómo lo recupero?**  
R: Reinicia la app. La comprobación se ejecuta una vez por sesión al arrancar.

**P: El diálogo de actualización dice "Error al descargar".**  
R: Es posible que la release de GitHub no tenga un asset APK adjunto, o que la conexión se haya interrumpido durante la descarga. El diálogo vuelve a abrir la página de la release en el navegador como alternativa.
