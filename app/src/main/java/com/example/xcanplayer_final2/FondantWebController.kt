package com.example.xcanplayer_final2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
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

    // 안드로이드가 결정한 목표 주소와 타입
    private var currentTargetUrl = ""
    private var isBibleMode = true

    inner class AndroidBotInterface {
        @JavascriptInterface
        fun requestPortrait() {
            val activity = webView.context as? Activity
            activity?.runOnUiThread {
                if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    onOrientationChangeNeeded()
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
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
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.setBackgroundColor(Color.TRANSPARENT)
        webView.addJavascriptInterface(AndroidBotInterface(), "AndroidBot")

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
                handleOrientationByUrl(url)
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                CookieManager.getInstance().flush()
                handleOrientationByUrl(url)
                applySpeed()
                injectTouchMagic()
                injectMasterBot()
            }
        }
    }

    private fun handleOrientationByUrl(url: String?) {
        val activity = webView.context as? Activity ?: return
        if (url == null) return

        val isLogin = listOf("login", "oauth", "auth", "signin", "kakao", "naver", "kauth", "nid").any { url.contains(it, ignoreCase = true) }

        if (isLogin) {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                onOrientationChangeNeeded()
            }
        } else if (url.contains("/series/") || url.contains("/watch") || url.contains("/play")) {
            if (activity.requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                onOrientationChangeNeeded()
            }
        }
    }

    fun loadSmartUrl(url: String) {
        // [원칙 1] 스케줄 판별은 MainActivity 몫. 여기서는 받은 주소를 끝까지 밀어붙입니다.
        currentTargetUrl = url
        isBibleMode = url.contains("00090228-5db3-dc44-3c29-52bcaf0002ce") || url.contains("bible") || url.contains("통독")
        webView.loadUrl(url)
    }

    fun isVideoPage(): Boolean {
        val url = webView.url ?: return false
        return url.contains("/series/") || url.contains("/watch") || url.contains("/play")
    }

    fun applySpeed() {
        val js = "javascript:(function() { window.androidSpeed = $currentSpeed; if(window.speedInt) clearInterval(window.speedInt); window.speedInt = setInterval(function() { var v = document.querySelector('video'); if(v && v.playbackRate != window.androidSpeed) v.playbackRate = window.androidSpeed; }, 1000); })();"
        webView.evaluateJavascript(js, null)
    }

    private fun injectTouchMagic() {
        val js = "javascript:(function() { document.addEventListener('touchstart', function(e) { if (e.touches.length > 0) { var touch = e.touches[0]; var event = new MouseEvent('mousemove', { 'view': window, 'bubbles': true, 'cancelable': true, 'clientX': touch.clientX, 'clientY': touch.clientY }); e.target.dispatchEvent(event); } }, {passive: true}); })();"
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

              // 로그인 클릭 시 안드로이드에 세로 모드 요청
              document.addEventListener('click', function(e) {
                  var txt = (e.target.innerText || '').replace(/\s/g, '');
                  if (txt.includes('로그인') || txt.includes('계정') || txt.includes('카카오') || txt.includes('네이버')) {
                      if(window.AndroidBot) window.AndroidBot.requestPortrait();
                  }
              }, true);

              window.botInt = setInterval(function() {
                var v = document.querySelector('video'); 
                var now = Date.now(); 
                var u = window.location.href;
                
                // 앱에서 전달받은 변수
                var targetUrl = '${currentTargetUrl}';
                var isBible = ${isBibleMode};
                
                if (v) {
                  if (window.androidSpeed && v.playbackRate !== window.androidSpeed) v.playbackRate = window.androidSpeed;
                  if (v.muted) { v.muted = false; v.volume = 1.0; }
                }

                // [원칙 1] 덜 엄격한 홈 화면 판별 (? 파라미터 무시)
                var cleanU = u.split('?')[0].replace(/\/$/, '');
                var isHome = (cleanU === 'https://www.fondant.kr' || u.includes('/main'));

                // [원칙 2] 홈 화면이면 텍스트 찾지 말고 무조건 목표 URL로 강제 이동!
                if (isHome && targetUrl && targetUrl !== '') {
                    if (now - window.lastPlayClickTime > 4000) {
                        window.location.href = targetUrl;
                        window.lastPlayClickTime = now;
                        return;
                    }
                }

                // [원칙 3 적용] 영상 페이지 판별 ( /play 포함 )
                if (u.includes('/series/') || u.includes('/watch') || u.includes('/play')) {
                    var tags = document.querySelectorAll('button, a, div, span, p');
                    
                    if (isBible) {
                        // ★ [원칙 4] 성경통독 전용: 다음회차 자동 실행
                        if (v && v.duration > 0 && (v.duration - v.currentTime <= 1.0 || v.ended)) {
                            for (var i = tags.length - 1; i >= 0; i--) {
                                var rawTxt = tags[i].innerText || '';
                                if (rawTxt.length < 30 && rawTxt.replace(/\s/g, '').includes('다음회차')) {
                                    triggerClick(tags[i]);
                                    return;
                                }
                            }
                        }

                        // 성경통독: 이어보기, 재생하기 버튼 누르기
                        if (!v || v.paused) {
                            if (now - window.lastPlayClickTime > 4000) { 
                                var clicked = false;
                                for (var i = tags.length - 1; i >= 0; i--) {
                                    var rawTxt = tags[i].innerText || '';
                                    if (rawTxt.length > 0 && rawTxt.length < 30) {
                                        var ct = rawTxt.replace(/\s/g, '');
                                        if (ct.includes('로그인')) {
                                            if (triggerClick(tags[i])) { if(window.AndroidBot) window.AndroidBot.requestPortrait(); clicked = true; }
                                        } else if (ct.includes('이어보기') || ct.includes('첫화보기') || ct.includes('재생')) {
                                            if (triggerClick(tags[i])) clicked = true;
                                        }
                                    }
                                    if(clicked) break;
                                }
                                if (clicked) { window.lastPlayClickTime = now; } 
                                else if (v && v.paused && v.currentTime < 1.0) { var p = v.play(); if(p !== undefined) p.catch(function(e){}); window.lastPlayClickTime = now; }
                            }
                        }

                    } else {
                        // ★ 생명의 삶 전용: 다음회차 없음. 최신 영상(목록) 링크만 찾아서 클릭.
                        if (!v || v.paused) {
                            if (now - window.lastPlayClickTime > 4000) {
                                var clicked = false;
                                
                                // 로그인 버튼 우선 확인
                                for (var i = tags.length - 1; i >= 0; i--) {
                                    if ((tags[i].innerText || '').replace(/\s/g, '').includes('로그인')) {
                                        if (triggerClick(tags[i])) { if(window.AndroidBot) window.AndroidBot.requestPortrait(); clicked = true; break; }
                                    }
                                }

                                if (!clicked && !u.includes('/watch') && !u.includes('/play')) {
                                    window.scrollBy(0, 600); 
                                    var links = document.querySelectorAll('a');
                                    var foundUrl = null;

                                    for (var i = 0; i < links.length; i++) {
                                        var h = links[i].href || '';
                                        // [원칙 3] /play 포괄 인식
                                        if (h.includes('/watch') || h.includes('/play')) {
                                            var rect = links[i].getBoundingClientRect();
                                            var absoluteY = rect.top + window.scrollY;

                                            if (absoluteY > 400 && rect.width > 0 && rect.height > 0) {
                                                foundUrl = h; 
                                                break; 
                                            }
                                        }
                                    }

                                    if (foundUrl) {
                                        window.location.href = foundUrl;
                                        window.lastPlayClickTime = now;
                                    }
                                } else if (!clicked && v && v.paused && v.currentTime < 1.0) {
                                    var p = v.play(); if(p !== undefined) p.catch(function(e){});
                                    window.lastPlayClickTime = now;
                                }
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