package com.fpkgi.manager.network

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object OrbisWebViewClient {

    private const val DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    // Intervalo de polling DOM — en vez de esperar 3500ms fijos,
    // revisamos cada POLL_MS hasta que los elementos estén presentes.
    private const val POLL_MS     = 400L
    private const val POLL_MAX    = 25        // 25 × 400ms = 10s máximo
    private const val FIRST_WAIT  = 600L      // espera inicial tras onPageFinished

    suspend fun extractPatchJson(
        context: Context,
        titleId: String,
        timeoutMs: Long = 20_000L
    ): String? = withTimeoutOrNull(timeoutMs) {
        val result = CompletableDeferred<String?>()

        withContext(Dispatchers.Main) {
            val webView  = createWebView(context)
            webView.clearCache(true)
            webView.clearHistory()
            WebStorage.getInstance().deleteAllData()

            var resolved   = false
            var pageLoads  = 0
            var pollCount  = 0
            val handler    = Handler(Looper.getMainLooper())

            fun complete(value: String?) {
                if (!resolved) {
                    resolved = true
                    webView.destroy()
                    result.complete(value)
                }
            }

            // Función de polling: se llama recursivamente cada POLL_MS
            // hasta obtener datos reales o agotar intentos.
            fun schedulePoll() {
                handler.postDelayed({
                    if (resolved) return@postDelayed
                    pollCount++
                    webView.evaluateJavascript(DOM_EXTRACTION_SCRIPT) { raw ->
                        if (resolved) return@evaluateJavascript
                        val cleaned = cleanJsString(raw)
                        when {
                            // DOM listo con datos → terminar
                            cleaned != null && cleaned != "LOADING" -> complete(cleaned)
                            // Cloudflare aún cargando, pero quedan reintentos
                            pollCount < POLL_MAX -> schedulePoll()
                            // Timeout de polls
                            else -> complete(null)
                        }
                    }
                }, POLL_MS)
            }

            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    pageLoads++
                }

                override fun onPageFinished(view: WebView, currentUrl: String?) {
                    if (resolved) return
                    // Cloudflare redirige → nueva carga, reiniciar contador de polls
                    pollCount = 0
                    // Espera inicial corta para que el framework JS hidrate el DOM
                    handler.postDelayed({ schedulePoll() }, FIRST_WAIT)
                }

                override fun onReceivedError(
                    view: WebView, request: WebResourceRequest?, error: WebResourceError?
                ) {
                    if (request?.isForMainFrame == true) complete(null)
                }
            }

            webView.loadUrl("https://orbispatches.com/$titleId")
        }

        result.await()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(context: Context): WebView =
        WebView(context.applicationContext).apply {
            settings.apply {
                javaScriptEnabled        = true
                domStorageEnabled        = true
                userAgentString          = DESKTOP_UA
                @Suppress("DEPRECATION") setSupportZoom(false)
                blockNetworkImage        = true
                loadsImagesAutomatically = false
                safeBrowsingEnabled      = false
                cacheMode                = WebSettings.LOAD_NO_CACHE
            }
            setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        }

    // Selectores idénticos al parser Python _parse_orbis_html():
    //   .patch-wrapper              → contenedor por parche
    //   a.patch-link[data-contentver] → versión
    //   .patch-container.latest     → marca el más reciente
    //   .col-auto.text-end [0,1,2]  → tamaño, FW, fecha
    //   a.changeinfo-preview[data-patchnotes-charcount > 0] → notas
    private val DOM_EXTRACTION_SCRIPT = """
(function() {
    try {
        if (document.title.indexOf('Just a moment') !== -1 ||
            document.querySelector('.cf-browser-verification') ||
            document.querySelector('#cf-wrapper')) {
            return JSON.stringify({ cloudflare: true });
        }
        var wrappers = document.querySelectorAll('.patch-wrapper');
        if (wrappers.length === 0) {
            return (document.body && document.body.innerText.length > 500)
                ? JSON.stringify({ patches: [], empty: true })
                : 'LOADING';
        }
        var gameName = document.title
            .replace(/^CUSA\d+[\s:–-]*/i, '')
            .replace(/\s*\|\s*ORBISPatches.*/i, '')
            .trim();
        var patches = [], seen = {};
        for (var i = 0; i < wrappers.length; i++) {
            var w = wrappers[i];
            var link = w.querySelector('a.patch-link[data-contentver]')
                    || w.querySelector('[data-contentver]');
            if (!link) continue;
            var ver = (link.getAttribute('data-contentver') || '').trim();
            if (!ver || seen[ver]) continue;
            seen[ver] = true;
            var cells = w.querySelectorAll('.col-auto.text-end');
            var notesEl = w.querySelector('a.changeinfo-preview[data-patchnotes-charcount]')
                       || w.querySelector('[data-patchnotes-charcount].changeinfo-preview');
            var notesText = '';
            if (notesEl && parseInt(notesEl.getAttribute('data-patchnotes-charcount')||'0',10) > 0) {
                var clone = notesEl.cloneNode(true);
                clone.querySelectorAll('span').forEach(function(s){ s.remove(); });
                notesText = clone.textContent.replace(/\n{3,}/g,'\n\n').trim();
            }
            patches.push({
                version:       ver,
                size:          cells[0] ? cells[0].textContent.trim() : '?',
                firmware:      cells[1] ? cells[1].textContent.trim() : '?',
                creation_date: cells[2] ? cells[2].textContent.trim() : '',
                notes:         notesText,
                is_latest:     !!w.querySelector('.patch-container.latest')
            });
        }
        patches.sort(function(a,b){
            var av=a.version.split('.').map(Number), bv=b.version.split('.').map(Number);
            for(var k=0;k<Math.max(av.length,bv.length);k++){
                var d=(bv[k]||0)-(av[k]||0); if(d) return d;
            } return 0;
        });
        if (patches.length > 0 && !patches.some(function(p){return p.is_latest;}))
            patches[0].is_latest = true;
        return JSON.stringify({ patches: patches, gameName: gameName });
    } catch(e) { return JSON.stringify({ error: String(e) }); }
})()
""".trimIndent()

    private fun cleanJsString(value: String?): String? {
        if (value == null || value == "null" || value.isBlank()) return null
        return if (value.startsWith("\"") && value.endsWith("\""))
            value.substring(1, value.length - 1)
                .replace("\\n", "\n").replace("\\\"", "\"")
                .replace("\\\\", "\\").replace("\\/", "/")
                .replace("\\t", "\t").replace("\\r", "")
        else value
    }
}
