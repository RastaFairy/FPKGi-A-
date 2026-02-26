# Política de seguridad

## Versiones con soporte

Solo la última versión publicada recibe correcciones de seguridad.

| Versión | Con soporte |
|---|---|
| 6.5.x (última) | ✅ |
| < 6.5 | ❌ |

## Reportar una vulnerabilidad

**No abras un issue público de GitHub para vulnerabilidades de seguridad.**

Reporta los problemas de seguridad de forma privada a través del sistema de avisos de seguridad integrado de GitHub:  
**Security → Report a vulnerability** en la página del repositorio.

Incluye:

- Descripción de la vulnerabilidad
- Pasos para reproducirla
- Impacto potencial
- Corrección sugerida si la tienes

Recibirás respuesta en un plazo de 7 días. Si la vulnerabilidad se confirma, se publicará una versión parcheada y se te mencionará en el changelog a menos que prefieras permanecer anónimo.

## Alcance

Esta app procesa ficheros JSON locales y se comunica con:

- `orbispatches.com` — scraping de metadatos de parches
- `api.github.com` — comprobación de actualizaciones (solo lectura, sin credenciales)
- Servidor FTP configurado por el usuario (red local PS4)
- URLs de descarga de PKGs configuradas por el usuario

No se almacenan ni transmiten credenciales, tokens ni datos personales a ningún servicio externo controlado por este proyecto.
