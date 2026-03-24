package com.example.xcanplayer_final2

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

class FondantWebController(
    private val webView: WebView,
    private val fullscreenContainer: FrameLayout,
    private val onOrientationChangeNeeded: () -> Unit
) {
    var customView: View? = null
    var customViewCallback: WebChromeClient.CustomViewCallback? = null
    var currentSpeed: Float = 1.0f

    @SuppressLint("SetJavaScriptEnabled")
    fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            textZoom = 100
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.setBackgroundColor(Color.TRANSPARENT)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                customView = view
                customViewCallback = callback
                fullscreenContainer.addView(view)
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE
                onOrientationChangeNeeded()
            }
            override fun onHideCustomView() {
                fullscreenContainer.removeView(customView)
                customView = null
                fullscreenContainer.visibility = View.GONE
                webView.visibility = View.VISIBLE
                customViewCallback?.onCustomViewHidden()
                onOrientationChangeNeeded()
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onOrientationChangeNeeded()
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                applySpeed()
                injectTouchMagic()
                injectMasterBot()
            }
        }
    }

    fun loadSmartUrl(url: String) {
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        webView.loadUrl(url)
    }

    fun applySpeed() {
        webView.evaluateJavascript("javascript:(function() { window.androidSpeed = $currentSpeed; setInterval(function() { var v = document.querySelector('video'); if(v && v.playbackRate != window.androidSpeed) v.playbackRate = window.androidSpeed; }, 1000); })();", null)
    }

    fun isVideoPage(): Boolean {
        val url = webView.url ?: return false
        return url.contains("/series/") || url.contains("/watch") || url.contains("/play/")
    }

    private fun injectTouchMagic() {
        val js = """
            javascript:(function() {
              document.addEventListener('touchstart', function(e) {
                if (e.touches.length > 0) {
                  var touch = e.touches[0];
                  var event = new MouseEvent('mousemove', { 'view': window, 'bubbles': true, 'cancelable': true, 'clientX': touch.clientX, 'clientY': touch.clientY });
                  e.target.dispatchEvent(event);
                }
              }, {passive: true});
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun injectMasterBot() {
        val js = """
            javascript:(function() {
              if(window.botInt) clearInterval(window.botInt);
              window.lastPlayClickTime = 0;
              
              function triggerClick(el) {
                if(!el) return false;
                try {
                  el.click();
                  var ev = new MouseEvent('click', {bubbles: true, cancelable: true, view: window});
                  el.dispatchEvent(ev);
                  return true;
                } catch(e){ return false; }
              }

              window.botInt = setInterval(function() {
                var v = document.querySelector('video'); 
                var now = Date.now(); 
                var u = window.location.href;
                
                if (v) {
                  if (window.androidSpeed && v.playbackRate !== window.androidSpeed) v.playbackRate = window.androidSpeed;
                  if (v.muted) { v.muted = false; v.volume = 1.0; }
                }

                if (!v || v.paused) {
                    if (u === 'https://www.fondant.kr/' || u === 'https://fondant.kr/' || u.endsWith('/main')) {
                        if (now - window.lastPlayClickTime > 4000) {
                            var links = document.querySelectorAll('a');
                            for(var i=0; i<links.length; i++) {
                                var txt = (links[i].innerText || '') + (links[i].querySelector('img') ? links[i].querySelector('img').alt : '');
                                var cleanTxt = txt.replace(/\s/g, '');
                                if(cleanTxt.includes('생명의삶') && links[i].href && links[i].href.includes('/series/')) { 
                                    window.location.href = links[i].href; 
                                    window.lastPlayClickTime = now; 
                                    return; 
                                }
                            }
                        }
                    }
                }

                if (u.includes('/series/') || u.includes('/watch') || u.includes('/play/')) {
                    var tags = document.querySelectorAll('button, a, div, span, p');
                    
                    if (v && v.duration > 0 && (v.duration - v.currentTime <= 1.0 || v.ended)) {
                        for (var i = tags.length - 1; i >= 0; i--) {
                            var rawTxt = tags[i].innerText || '';
                            if (rawTxt.length < 30 && rawTxt.replace(/\s/g, '').includes('다음회차')) {
                                triggerClick(tags[i]);
                            }
                        }
                        return;
                    }

                    if (!v || v.paused) {
                        if (now - window.lastPlayClickTime > 4000) { 
                            var clicked = false;
                            var isBible = u.includes('00090228-5db3-dc44-3c29-52bcaf0002ce');

                            if (isBible) {
                                /* [트랙 A: 성경통독] 정상 작동하던 보라색 '이어보기' 버튼을 직접 누릅니다. (그대로 유지) */
                                for (var i = tags.length - 1; i >= 0; i--) {
                                    var rawTxt = tags[i].innerText || '';
                                    if (rawTxt.length > 0 && rawTxt.length < 30) {
                                        var ct = rawTxt.replace(/\s/g, '');
                                        if (ct.includes('이어보기') || ct.includes('첫화보기')) {
                                            if (triggerClick(tags[i])) clicked = true;
                                        }
                                    }
                                }   
                            } else {
                                /* ★ [트랙 B: 생명의 삶] 기하학적 좌표 분석으로 어제 영상(배너) 완벽 무시! ★ */
                                
                                // 화면 아래의 목록을 띄우기 위해 스크롤을 살짝 내립니다.
                                window.scrollBy(0, 600); 

                                var links = document.querySelectorAll('a');
                                var targetUrl = null;

                                for (var i = 0; i < links.length; i++) {
                                    var h = links[i].href || '';
                                    if (h.includes('/watch') || h.includes('/play/')) {
                                        // 해당 링크의 화면상 수직 위치(Y좌표)를 계산합니다.
                                        var rect = links[i].getBoundingClientRect();
                                        var absoluteY = rect.top + window.scrollY;

                                        // 핵심: 화면 상단(0~400px)에 있는 거대한 이어보기 배너 영역은 모조리 쌩깝니다!
                                        // 400px 아래(목록 부분)에서 발견되는 '가장 첫 번째' 링크가 바로 오늘자 최신 영상입니다.
                                        if (absoluteY > 400 && rect.width > 0 && rect.height > 0) {
                                            targetUrl = h; 
                                            break; 
                                        }
                                    }
                                }

                                if (targetUrl) {
                                    // 좌표 계산으로 훔쳐낸 진짜 최신 영상 주소로 순간이동!
                                    window.location.href = targetUrl;
                                    clicked = true;
                                } else {
                                    // 만약 목록이 진짜 1도 안 보이면 최후의 수단으로 뱃지 타격
                                    for(var i=0; i<tags.length; i++) {
                                        var ct = (tags[i].innerText || '').trim();
                                        if (/^\d{1,2}:\d{2}(:\d{2})?$/.test(ct) || /^\d+분$/.test(ct)) {
                                            if(triggerClick(tags[i])) { clicked = true; break; }
                                        }
                                    }
                                }
                            }

                            if (clicked) {
                                window.lastPlayClickTime = now;
                            } else if (v && v.paused && v.currentTime < 1.0) {
                                var p = v.play(); if(p !== undefined) p.catch(function(e){});
                                window.lastPlayClickTime = now;
                            }
                        }
                    }
                }
              }, 1500);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }
}