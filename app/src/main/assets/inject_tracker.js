/**
 * Mindful Hub - Mobile Tracker Script for WebView
 * Injected into m.youtube.com, www.tiktok.com, m.facebook.com
 * 
 * Features:
 * 1. Auto-detect platform: youtube | facebook | tiktok
 * 2. SPA Navigation Hooking (pushState, replaceState, popstate, yt-navigate-finish, interval fallback)
 * 3. Shorts/Reels/TikTok Swipe Tracking (totalSwipes, validViews >= 2s, loopViews, impulsiveCount)
 * 4. Facebook Feed Scroll & Deep Read Tracking
 * 5. Long Video Completion Rate & Impulsive Skips (YouTube / Facebook Watch)
 * 6. Reload / F5 Tracking
 * 7. Active vs Passive Time Tracking
 * 8. Grayscale Demotivation Mode
 * 9. Native Bridge Sync via window.MindfulBridge
 */

(function () {
    if (window.__MINDFUL_HUB_INJECTED__) {
        console.log("[MindfulHub] Tracker already injected, refreshing state.");
        return;
    }
    window.__MINDFUL_HUB_INJECTED__ = true;

    // Detect Platform
    var hostname = window.location.hostname || "";
    var currentPlatform = "youtube";
    if (hostname.indexOf("tiktok.com") !== -1) {
        currentPlatform = "tiktok";
    } else if (hostname.indexOf("facebook.com") !== -1) {
        currentPlatform = "facebook";
    }

    console.log("[MindfulHub] Initializing Tracker for platform:", currentPlatform);

    // Platform State
    var state = {
        platform: currentPlatform,
        totalSwipes: 0,
        validViews: 0,
        impulsiveCount: 0,
        loopViews: 0,
        activeSeconds: 0,
        passiveSeconds: 0,
        reloadCount: 0,
        currentShortId: null,
        isShortsPage: false,
        lastInteractionTime: Date.now()
    };

    var validViewTimer = null;
    var currentVideoEl = null;
    var lastVideoTime = 0;
    var IDLE_TIMEOUT_MS = 30000;

    // Long video state
    var currentLongVideoId = null;
    var longWatchedSeconds = 0;
    var longDuration = 0;
    var longWatchInterval = null;

    // Pomodoro Focus state (updated via window.__setPomodoroFocusState)
    window.__pomodoroIsFocus = false;
    window.__pomodoroFocusMode = "music"; // "music" | "study"
    window.__setPomodoroFocusState = function (isFocus, mode) {
        window.__pomodoroIsFocus = !!isFocus;
        if (mode) window.__pomodoroFocusMode = mode;
        logBridge("Pomodoro focus state updated: isFocus=" + window.__pomodoroIsFocus + ", mode=" + window.__pomodoroFocusMode);
    };

    var studyCheckInTimer = null;
    var checkedInVideos = {};

    function isMusicVideo(title, channelName) {
        if (currentPlatform !== "youtube") return false;
        try {
            var activeChips = document.querySelectorAll(
                "yt-chip-cloud-chip-renderer[selected], #chips yt-chip-cloud-chip-renderer[aria-selected='true'], ytm-chip-cloud-chip-renderer[selected], ytm-chip-cloud-chip-renderer[aria-selected='true']"
            );
            for (var i = 0; i < activeChips.length; i++) {
                var text = (activeChips[i].textContent || "").trim().toLowerCase();
                if (text === "âm nhạc" || text === "nhạc" || text === "music") {
                    return true;
                }
            }
        } catch (_) {}

        var ch = (channelName || "").toLowerCase();
        if (ch.indexOf("- topic") !== -1 || ch.indexOf("- chủ đề") !== -1 || ch.indexOf("official music") !== -1 || ch.indexOf("records") !== -1 || ch.indexOf("vevo") !== -1) {
            return true;
        }

        if (document.querySelector("[aria-label*='nghệ sĩ'], [aria-label*='Artist'], [class*='badge-style-type-artist']")) {
            return true;
        }

        var t = (title || "").toLowerCase();
        var musicRegex = /\b(music|lofi|chill|playlist|nhạc|bài hát|soundtrack|acoustic|instrumental|remix|piano|ambient|audio|mv|official music video|karaoke|beat|lyric|lyrics|mashup|ost|medley|guitar|synthwave|relaxing)\b/i;
        return musicRegex.test(t) || musicRegex.test(ch);
    }

    // Helper: Safe Bridge Communication
    function logBridge(message) {
        if (window.MindfulBridge && typeof window.MindfulBridge.log === "function") {
            try { window.MindfulBridge.log(message); } catch (e) {}
        } else {
            console.log("[MindfulHub]", message);
        }
    }

    function syncStats() {
        if (!window.MindfulBridge) return;
        try {
            var payload = JSON.stringify({
                platform: currentPlatform,
                totalSwipes: state.totalSwipes,
                validViews: state.validViews,
                impulsiveCount: state.impulsiveCount,
                loopViews: state.loopViews,
                activeSeconds: state.activeSeconds,
                passiveSeconds: state.passiveSeconds,
                reloadCount: state.reloadCount,
                currentShortId: state.currentShortId || ""
            });

            if (typeof window.MindfulBridge.updatePlatformStats === "function") {
                window.MindfulBridge.updatePlatformStats(currentPlatform, payload);
            } else if (typeof window.MindfulBridge.updateStats === "function") {
                window.MindfulBridge.updateStats(payload);
            }
        } catch (e) {
            console.error("[MindfulHub] Error syncing stats:", e);
        }
    }

    // --- Reload / F5 Detection ---
    try {
        var navEntries = performance.getEntriesByType("navigation");
        var isReload = (navEntries && navEntries.length > 0 && navEntries[0].type === "reload") ||
                       (performance.navigation && performance.navigation.type === 1);
        if (isReload) {
            state.reloadCount++;
            logBridge("Reload detected on " + currentPlatform);
            if (window.MindfulBridge && typeof window.MindfulBridge.onReload === "function") {
                window.MindfulBridge.onReload(currentPlatform);
            }
        }
    } catch (e) {}

    // --- Interaction & Active Time Tracking ---
    function registerUserActivity() {
        state.lastInteractionTime = Date.now();
    }

    ["touchstart", "touchmove", "touchend", "click", "scroll", "keydown"].forEach(function (evt) {
        window.addEventListener(evt, registerUserActivity, { passive: true });
    });

    setInterval(function () {
        if (document.hidden) return;
        var now = Date.now();
        if (now - state.lastInteractionTime <= IDLE_TIMEOUT_MS) {
            state.activeSeconds++;
        } else {
            state.passiveSeconds++;
        }
        if (state.activeSeconds % 5 === 0) {
            syncStats();
        }
    }, 1000);

    // --- Short / Reel / TikTok ID Extraction ---
    function extractVideoId() {
        var path = location.pathname || "";
        if (currentPlatform === "youtube") {
            var m = path.match(/\/shorts\/([a-zA-Z0-9_-]+)/);
            if (m && m[1]) return { type: "short", id: m[1] };
            if (path.indexOf("/watch") !== -1) {
                var p = new URLSearchParams(location.search);
                var v = p.get("v");
                if (v) return { type: "long", id: v };
            }
            // Check active Short in mobile DOM if URL hasn't pushed
            var activeReel = document.querySelector("ytm-reel-item-renderer[is-active], [aria-hidden='false'] [class*='reel-item']");
            if (activeReel) {
                var reelLink = activeReel.querySelector("a[href*='/shorts/']");
                if (reelLink) {
                    var rm = reelLink.getAttribute("href").match(/\/shorts\/([a-zA-Z0-9_-]+)/);
                    if (rm && rm[1]) return { type: "short", id: rm[1] };
                }
            }
            if (path.indexOf("/shorts") !== -1) {
                return { type: "short", id: "yt_short_" + (state.totalSwipes + 1) };
            }
        } else if (currentPlatform === "tiktok") {
            var tm = path.match(/\/video\/([0-9]+)/);
            if (tm && tm[1]) return { type: "short", id: tm[1] };
            // On TikTok feed, each item has a distinct container or video
            var activeFeedItem = document.querySelector("[data-e2e='recommend-list-item-container'], [data-e2e='feed-item'], [class*='DivItemContainerV2']");
            if (activeFeedItem) {
                var feedLink = activeFeedItem.querySelector("a[href*='/video/']");
                if (feedLink) {
                    var ftm = feedLink.getAttribute("href").match(/\/video\/([0-9]+)/);
                    if (ftm && ftm[1]) return { type: "short", id: ftm[1] };
                }
                var feedId = activeFeedItem.getAttribute("id") || activeFeedItem.getAttribute("data-video-id");
                if (feedId) return { type: "short", id: "tt_" + feedId };
            }
            if (path.indexOf("/@") !== -1 || path === "/" || path.indexOf("/foryou") !== -1) {
                return { type: "short", id: "tiktok_v" + (state.totalSwipes + 1) };
            }
        } else if (currentPlatform === "facebook") {
            var fm = path.match(/\/reels?\/([a-zA-Z0-9_-]+)/);
            if (fm && fm[1]) return { type: "short", id: fm[1] };
            if (path.indexOf("/watch") !== -1 || path.indexOf("/videos/") !== -1) {
                return { type: "long", id: "fb_watch_" + path };
            }
            if (path.indexOf("/reel") !== -1) {
                return { type: "short", id: "fb_reel_" + (state.totalSwipes + 1) };
            }
            if (path === "/" || path === "") {
                return { type: "feed", id: "fb_feed" };
            }
        }
        return null;
    }

    // Video play listener: When a new video starts playing, detect if it's a new video stream!
    var lastObservedSrc = "";
    document.addEventListener("playing", function (e) {
        var v = e.target;
        if (!v || v.tagName !== "VIDEO") return;
        var src = v.currentSrc || v.src || "";
        if (src && src !== lastObservedSrc) {
            lastObservedSrc = src;
            logBridge("New video stream started: " + src.substring(0, 40));
            setTimeout(onRouteOrDomChanged, 250);
        }
    }, true);

    // --- Short Loop Tracking ---
    function attachLoopTracker() {
        setTimeout(function () {
            var video = document.querySelector("video");
            if (!video || video === currentVideoEl) return;
            currentVideoEl = video;
            lastVideoTime = 0;

            video.addEventListener("timeupdate", function () {
                if (!state.isShortsPage) return;
                var current = video.currentTime;
                var duration = video.duration;
                if (duration > 0 && lastVideoTime > duration * 0.8 && current < 1.0) {
                    state.loopViews++;
                    logBridge(currentPlatform + " video looped. Total loops: " + state.loopViews);
                    if (window.MindfulBridge && typeof window.MindfulBridge.onLoopView === "function") {
                        window.MindfulBridge.onLoopView(currentPlatform, state.currentShortId || "", state.loopViews);
                    }
                    syncStats();
                }
                lastVideoTime = current;
            });

            video.addEventListener("ended", function () {
                if (!state.isShortsPage) return;
                state.loopViews++;
                if (window.MindfulBridge && typeof window.MindfulBridge.onLoopView === "function") {
                    window.MindfulBridge.onLoopView(currentPlatform, state.currentShortId || "", state.loopViews);
                }
                syncStats();
            });
        }, 800);
    }

    // --- Metadata Extraction for On-Device Categorization ---
    function extractVideoMetadata(videoId, isShort) {
        var title = "";
        var channel = "";

        try {
            if (currentPlatform === "youtube") {
                if (isShort) {
                    var activeSlide = document.querySelector("ytm-reel-item-renderer[is-active], ytm-shorts-player[is-active], [is-active] ytm-reel-player-overlay-renderer, ytm-reel-player-overlay-renderer");
                    var titleEl = activeSlide ? activeSlide.querySelector(".reel-player-header-renderer-title, ytm-reel-player-header-renderer .title, ytm-reel-player-header-renderer .yt-core-attributed-string, h2.title, [class*='reel-player-header'] h2, [role='heading'], .shorts-video-title, #overlay-title") : null;
                    if (!titleEl) {
                        titleEl = document.querySelector(".reel-player-header-renderer-title, ytm-reel-player-header-renderer .title, ytm-reel-player-header-renderer .yt-core-attributed-string, h2.title, [class*='reel-player-header'] h2, [class*='reel-player-header'] span, #overlay-title, .shorts-video-title, [role='heading']");
                    }
                    if (titleEl && titleEl.innerText) title = titleEl.innerText.trim();

                    var channelEl = activeSlide ? activeSlide.querySelector(".channel-name, ytm-channel-name, [class*='reel-channel-name'], [class*='channel-title']") : null;
                    if (!channelEl) {
                        channelEl = document.querySelector(".channel-name, ytm-channel-name, [class*='reel-channel-name'], [class*='channel-title'], ytm-reel-player-header-renderer [class*='channel']");
                    }
                    if (channelEl && channelEl.innerText) channel = channelEl.innerText.trim();
                } else {
                    var titleEl = document.querySelector("h1.title, .slim-video-metadata-title, .watch-title, h1, [role='heading']");
                    if (titleEl && titleEl.innerText) title = titleEl.innerText.trim();
                    var channelEl = document.querySelector("ytm-slim-owner-renderer .owner-channel-name, .channel-name, .ytm-channel-title");
                    if (channelEl && channelEl.innerText) channel = channelEl.innerText.trim();
                }
            } else if (currentPlatform === "tiktok") {
                var descEl = document.querySelector("[data-e2e='browse-video-desc'], [data-e2e='video-desc'], h1, [class*='video-desc']");
                if (descEl && descEl.innerText) title = descEl.innerText.trim();
                var userEl = document.querySelector("[data-e2e='browse-user-title'], [data-e2e='browse-username'], [data-e2e='user-title'], [class*='author']");
                if (userEl && userEl.innerText) channel = userEl.innerText.trim();
            } else if (currentPlatform === "facebook") {
                var fbTitle = document.querySelector("[data-sigil='m-video-title'], h3, span[dir='auto'], [class*='story_body']");
                if (fbTitle && fbTitle.innerText) title = fbTitle.innerText.trim();
                var fbUser = document.querySelector("strong a, a.actor-link, [role='article'] strong");
                if (fbUser && fbUser.innerText) channel = fbUser.innerText.trim();
            }
        } catch (e) {}

        // Fallbacks
        if (!title && document.title) {
            var docT = document.title.replace(" - YouTube", "").replace(" | TikTok", "").replace(" | Facebook", "").trim();
            if (docT.toLowerCase() !== "youtube" && docT.toLowerCase() !== "shorts" && docT.toLowerCase() !== "tiktok" && docT.toLowerCase() !== "facebook") {
                title = docT;
            }
        }
        if (!title || title.toLowerCase() === "youtube") {
            title = (isShort ? "Short #" : "Video #") + (videoId ? videoId.substring(0, 11) : "MXH");
        }
        if (!channel) {
            channel = currentPlatform === "youtube" ? "YouTube Creator" : (currentPlatform === "facebook" ? "Facebook" : "TikTok");
        }

        return { title: title, channel: channel };
    }

    // --- Long Video Tracking ---
    function handleLongVideo(videoId) {
        if (videoId === currentLongVideoId) return;
        endLongVideo();

        currentLongVideoId = videoId;
        longWatchedSeconds = 0;
        longDuration = 0;

        // Extract metadata and classify early at 3s mark
        setTimeout(function () {
            if (currentLongVideoId === videoId) {
                var meta = extractVideoMetadata(videoId, false);
                if (window.MindfulBridge && typeof window.MindfulBridge.onVideoWatched === "function") {
                    window.MindfulBridge.onVideoWatched(currentPlatform, videoId, meta.title, meta.channel, Math.round(longDuration), Math.round(longWatchedSeconds || 3));
                }
            }
        }, 3000);

        // 10s Study Focus Check-in
        if (studyCheckInTimer) {
            clearTimeout(studyCheckInTimer);
            studyCheckInTimer = null;
        }
        if (window.__pomodoroIsFocus && window.__pomodoroFocusMode === "study") {
            studyCheckInTimer = setTimeout(function () {
                if (currentLongVideoId === videoId && !checkedInVideos[videoId]) {
                    checkedInVideos[videoId] = true;
                    var meta = extractVideoMetadata(videoId, false);
                    logBridge("10s Study Check-in triggered for: " + meta.title);
                    if (window.MindfulBridge && typeof window.MindfulBridge.onStudyCheckInRequired === "function") {
                        window.MindfulBridge.onStudyCheckInRequired(videoId, meta.title);
                    }
                }
            }, 10000);
        }

        longWatchInterval = setInterval(function () {
            var v = document.querySelector("video");
            if (v && !v.paused && !document.hidden) {
                longWatchedSeconds++;
                if (!longDuration && v.duration) longDuration = v.duration;
            }
        }, 1000);
    }

    function endLongVideo() {
        if (!currentLongVideoId) return;
        if (studyCheckInTimer) {
            clearTimeout(studyCheckInTimer);
            studyCheckInTimer = null;
        }
        if (longWatchInterval) {
            clearInterval(longWatchInterval);
            longWatchInterval = null;
        }

        var watched = Math.round(longWatchedSeconds);
        var duration = Math.round(longDuration);
        var meta = extractVideoMetadata(currentLongVideoId, false);

        // Music Focus handling: If in Pomodoro Music Focus mode and video is music, record separately
        var isFocusMusic = (window.__pomodoroIsFocus && window.__pomodoroFocusMode === "music");
        var isMusic = isMusicVideo(meta.title, meta.channel);
        if (isFocusMusic && isMusic) {
            if (watched >= 5) {
                logBridge(currentPlatform + " music video recorded in Music Focus: " + meta.title + " (" + watched + "s)");
                if (window.MindfulBridge && typeof window.MindfulBridge.onMusicVideo === "function") {
                    window.MindfulBridge.onMusicVideo(currentPlatform, currentLongVideoId, meta.title, watched);
                }
            }
            currentLongVideoId = null;
            longWatchedSeconds = 0;
            longDuration = 0;
            return;
        }

        if (watched >= 3) {
            var pct = duration > 0 ? (watched / duration) : 0;
            var isUseful = pct >= 0.80 || watched >= 180;
            var isImpulsive = duration > 60 && pct < 0.15;

            logBridge(currentPlatform + " long video ended: watched=" + watched + "s, isUseful=" + isUseful + ", isImpulsive=" + isImpulsive);
            if (window.MindfulBridge && typeof window.MindfulBridge.onLongVideo === "function") {
                window.MindfulBridge.onLongVideo(currentPlatform, isUseful, isImpulsive);
            }
            if (window.MindfulBridge && typeof window.MindfulBridge.onVideoWatched === "function") {
                window.MindfulBridge.onVideoWatched(currentPlatform, currentLongVideoId, meta.title, meta.channel, duration, watched);
            }
        }

        currentLongVideoId = null;
        longWatchedSeconds = 0;
        longDuration = 0;
    }

    // --- Main Route & Navigation Handler ---
    function onRouteOrDomChanged() {
        var info = extractVideoId();

        if (info && info.type === "short") {
            endLongVideo();
            state.isShortsPage = true;

            var actType = currentPlatform === "facebook" ? "reels" : (currentPlatform === "tiktok" ? "tiktok" : "shorts");
            if (window.MindfulBridge && typeof window.MindfulBridge.onActivityChanged === "function") {
                window.MindfulBridge.onActivityChanged(currentPlatform, actType, info.id || "");
            }

            if (info.id !== state.currentShortId) {
                // If previous short was swiped away before reaching 2 seconds, it was an impulsive skip!
                if (validViewTimer) {
                    clearTimeout(validViewTimer);
                    validViewTimer = null;
                    state.impulsiveCount++;
                    var prevMeta = extractVideoMetadata(state.currentShortId, true);
                    if (window.MindfulBridge && typeof window.MindfulBridge.onVideoWatched === "function") {
                        window.MindfulBridge.onVideoWatched(currentPlatform, state.currentShortId || "", prevMeta.title, prevMeta.channel, 0, 1);
                    }
                }

                state.currentShortId = info.id;
                state.totalSwipes++;
                logBridge(currentPlatform + " short swipe: ID=" + info.id + " (Total=" + state.totalSwipes + ")");

                if (window.MindfulBridge && typeof window.MindfulBridge.onShortSwipe === "function") {
                    window.MindfulBridge.onShortSwipe(currentPlatform, info.id, state.totalSwipes);
                }
                syncStats();

                // Valid View Timer (>= 2s)
                var trackedId = info.id;
                validViewTimer = setTimeout(function () {
                    if (state.isShortsPage && state.currentShortId === trackedId) {
                        state.validViews++;
                        logBridge(currentPlatform + " valid deep view (>=2s) confirmed for " + trackedId);
                        if (window.MindfulBridge && typeof window.MindfulBridge.onValidView === "function") {
                            window.MindfulBridge.onValidView(currentPlatform, trackedId, state.validViews);
                        }
                        // Report watched video for on-device categorization
                        var meta = extractVideoMetadata(trackedId, true);
                        if (window.MindfulBridge && typeof window.MindfulBridge.onVideoWatched === "function") {
                            window.MindfulBridge.onVideoWatched(currentPlatform, trackedId, meta.title, meta.channel, 0, 3);
                        }
                        syncStats();
                    }
                    validViewTimer = null;
                }, 2000);

                attachLoopTracker();
            }
        } else if (info && info.type === "long") {
            state.isShortsPage = false;
            if (validViewTimer) {
                clearTimeout(validViewTimer);
                validViewTimer = null;
            }
            if (window.MindfulBridge && typeof window.MindfulBridge.onActivityChanged === "function") {
                window.MindfulBridge.onActivityChanged(currentPlatform, "long_video", info.id || "");
            }
            handleLongVideo(info.id);
        } else if (info && info.type === "feed") {
            state.isShortsPage = false;
            endLongVideo();
            if (validViewTimer) {
                clearTimeout(validViewTimer);
                validViewTimer = null;
            }
            if (window.MindfulBridge && typeof window.MindfulBridge.onActivityChanged === "function") {
                window.MindfulBridge.onActivityChanged(currentPlatform, "feed", "feed");
            }
            // Feed scroll detected
            if (window.MindfulBridge && typeof window.MindfulBridge.onFeedScroll === "function") {
                window.MindfulBridge.onFeedScroll(false);
            }
        } else {
            state.isShortsPage = false;
            endLongVideo();
            if (window.MindfulBridge && typeof window.MindfulBridge.onActivityChanged === "function") {
                window.MindfulBridge.onActivityChanged(currentPlatform, "browse", location.pathname || "");
            }
        }
    }

    // --- Scroll Behavior: Auto-hide / Auto-show Navigation Bars (YouTube Style) ---
    var lastScrollY = window.scrollY || 0;
    var ticking = false;
    var isNavHidden = false;

    function injectScrollStyles() {
        if (document.getElementById("mindful-autohide-style")) return;
        var style = document.createElement("style");
        style.id = "mindful-autohide-style";
        style.textContent = [
            "/* YouTube Mobile Navigation Auto-Hide */",
            "ytm-header-bar, #header-bar, header.header-bar {",
            "    transition: transform 0.28s cubic-bezier(0.4, 0.0, 0.2, 1) !important;",
            "    will-change: transform;",
            "}",
            "ytm-pivot-bar-renderer, #pivot-bar, nav.pivot-bar {",
            "    transition: transform 0.28s cubic-bezier(0.4, 0.0, 0.2, 1) !important;",
            "    will-change: transform;",
            "}",
            "html.mindful-nav-hidden ytm-header-bar,",
            "html.mindful-nav-hidden #header-bar,",
            "html.mindful-nav-hidden header.header-bar {",
            "    transform: translateY(-100%) !important;",
            "}",
            "html.mindful-nav-hidden ytm-pivot-bar-renderer,",
            "html.mindful-nav-hidden #pivot-bar,",
            "html.mindful-nav-hidden nav.pivot-bar {",
            "    transform: translateY(120%) !important;",
            "}",
            "/* Facebook Mobile Auto-Hide */",
            "html.mindful-nav-hidden #header,",
            "html.mindful-nav-hidden .m-header,",
            "html.mindful-nav-hidden [role='banner'] {",
            "    transform: translateY(-100%) !important;",
            "    transition: transform 0.28s ease !important;",
            "}",
            "/* TikTok Mobile Nav Auto-Hide */",
            "html.mindful-nav-hidden .tiktok-bottom-nav,",
            "html.mindful-nav-hidden [data-e2e='bottom-nav'] {",
            "    transform: translateY(120%) !important;",
            "    transition: transform 0.28s ease !important;",
            "}"
        ].join("\n");
        (document.head || document.documentElement).appendChild(style);
    }
    injectScrollStyles();

    function onWindowScroll() {
        var currentScrollY = window.scrollY || document.documentElement.scrollTop || 0;
        var diff = currentScrollY - lastScrollY;

        if (diff > 12 && currentScrollY > 50 && !isNavHidden) {
            isNavHidden = true;
            document.documentElement.classList.add("mindful-nav-hidden");
            if (window.MindfulBridge && typeof window.MindfulBridge.onScrollDirection === "function") {
                window.MindfulBridge.onScrollDirection(false);
            }
        } else if (diff < -12 && isNavHidden) {
            isNavHidden = false;
            document.documentElement.classList.remove("mindful-nav-hidden");
            if (window.MindfulBridge && typeof window.MindfulBridge.onScrollDirection === "function") {
                window.MindfulBridge.onScrollDirection(true);
            }
        }

        lastScrollY = Math.max(0, currentScrollY);
        ticking = false;
    }

    window.addEventListener("scroll", function () {
        if (!ticking) {
            window.requestAnimationFrame(onWindowScroll);
            ticking = true;
        }
    }, { passive: true });

    // Hook Navigation Events
    var originalPushState = history.pushState;
    history.pushState = function () {
        var ret = originalPushState.apply(this, arguments);
        window.dispatchEvent(new Event("mindful-nav-change"));
        return ret;
    };

    var originalReplaceState = history.replaceState;
    history.replaceState = function () {
        var ret = originalReplaceState.apply(this, arguments);
        window.dispatchEvent(new Event("mindful-nav-change"));
        return ret;
    };

    window.addEventListener("popstate", onRouteOrDomChanged);
    window.addEventListener("hashchange", onRouteOrDomChanged);
    window.addEventListener("mindful-nav-change", onRouteOrDomChanged);
    window.addEventListener("yt-navigate-finish", onRouteOrDomChanged);
    window.addEventListener("yt-page-data-updated", onRouteOrDomChanged);

    var lastCheckedHref = location.href;
    setInterval(function () {
        if (location.href !== lastCheckedHref) {
            lastCheckedHref = location.href;
            onRouteOrDomChanged();
        }
    }, 400);

    // Grayscale Demotivation Mode (Phase 5)
    function injectGrayscaleStyle() {
        if (document.getElementById("mindful-grayscale-style")) return;
        var style = document.createElement("style");
        style.id = "mindful-grayscale-style";
        style.textContent = "html.mindful-grayscale-active { filter: grayscale(100%) !important; transition: filter 0.5s ease-in-out !important; }";
        (document.head || document.documentElement).appendChild(style);
    }
    injectGrayscaleStyle();

    // --- Logo Tap & Pull-to-refresh Detection (Record F5 / Dopamine seek) ---
    function isLogoElement(el) {
        if (!el || !el.closest) return false;

        // Strictly exclude video player, audio controls, mute buttons, search bar, and form inputs
        if (el.closest(".html5-video-player, video, ytm-search-box, form.search-form, #search-form, input, textarea, button[aria-label*='Search'], button[aria-label*='Tìm kiếm'], .topbar-search-button, .ytp-mute-button, .ytp-button, .player-control-button, .sound-button")) {
            return false;
        }

        if (currentPlatform === "youtube") {
            return !!el.closest("a#logo, a.header-bar-logo, ytm-home-logo, ytd-topbar-logo-renderer");
        } else if (currentPlatform === "facebook") {
            return !!el.closest("a[aria-label='Facebook'], svg[aria-label='Facebook'], a[href='/?ref=logo'], .m-header-logo");
        } else if (currentPlatform === "tiktok") {
            return !!el.closest("a[data-e2e='tiktok-logo'], a.tiktok-logo");
        }
        return false;
    }

    var lastLogoClickTime = 0;
    document.addEventListener("click", function (e) {
        var el = e.target;
        if (isLogoElement(el)) {
            var now = Date.now();
            if (now - lastLogoClickTime > 3000) {
                lastLogoClickTime = now;
                state.reloadCount++;
                logBridge(currentPlatform + " logo clicked -> recorded dopamine seek reload attempt");
                if (window.MindfulBridge && typeof window.MindfulBridge.onReload === "function") {
                    window.MindfulBridge.onReload(currentPlatform);
                }
            }
            // IMPORTANT: Never call window.location.reload()! The site handles SPA navigation naturally.
        }
    }, true);

    // --- Pull Down at Top of Feed to Record Reload ---
    var touchStartY = 0;
    var touchStartX = 0;
    var isEligibleForPull = false;

    window.addEventListener("touchstart", function (e) {
        if (e.touches && e.touches.length === 1) {
            // Never enable pull-to-refresh on Shorts, video watch, or TikTok
            if (state.isShortsPage || currentPlatform === "tiktok" || location.pathname.indexOf("/watch") !== -1) {
                isEligibleForPull = false;
                return;
            }
            var target = e.target;
            if (target && target.closest && target.closest(".html5-video-player, video, ytm-search-box, input, textarea")) {
                isEligibleForPull = false;
                return;
            }
            touchStartY = e.touches[0].clientY;
            touchStartX = e.touches[0].clientX;
            var scrollY = window.scrollY || document.documentElement.scrollTop || 0;
            isEligibleForPull = (scrollY <= 2);
        }
    }, { passive: true });

    window.addEventListener("touchmove", function (e) {
        if (isEligibleForPull && e.touches && e.touches.length === 1) {
            if (state.isShortsPage || currentPlatform === "tiktok" || location.pathname.indexOf("/watch") !== -1) {
                isEligibleForPull = false;
                return;
            }
            var diffY = e.touches[0].clientY - touchStartY;
            var diffX = Math.abs(e.touches[0].clientX - touchStartX);
            if (diffY > 180 && diffX < 50) {
                isEligibleForPull = false;
                logBridge("Pull-to-refresh detected on " + currentPlatform);
                if (window.MindfulBridge && typeof window.MindfulBridge.onReload === "function") {
                    window.MindfulBridge.onReload(currentPlatform);
                }
                // Do not force window.location.reload() — YouTube/FB handles feed refresh natively
            }
        }
    }, { passive: true });

    window.__setGrayscale = function (enabled) {
        injectGrayscaleStyle();
        var html = document.documentElement;
        if (enabled) {
            if (!html.classList.contains("mindful-grayscale-active")) {
                html.classList.add("mindful-grayscale-active");
            }
        } else {
            if (html.classList.contains("mindful-grayscale-active")) {
                html.classList.remove("mindful-grayscale-active");
            }
        }
    };

    onRouteOrDomChanged();
    logBridge("Mindful Multi-platform Tracker successfully initialized for " + currentPlatform);
})();
