// Header scroll effect
window.addEventListener('scroll', function () {
    const header = document.querySelector('header');
    if (window.scrollY > 50) {
        header.classList.add('scrolled');
    } else {
        header.classList.remove('scrolled');
    }
});

// Mobile Navigation Menu Toggle
document.addEventListener('DOMContentLoaded', () => {
    const hamburgerBtn = document.querySelector('.hamburger-menu-button');
    const closeMenuBtn = document.querySelector('.close-menu-button');
    const mobileNavMenu = document.querySelector('.mobile-nav-menu');
    const mobileNavOverlay = document.querySelector('.mobile-nav-overlay');
    const mobileNavLinks = document.querySelectorAll('.mobile-nav-links ul li a');

    if (hamburgerBtn && mobileNavMenu && closeMenuBtn && mobileNavOverlay) {
        hamburgerBtn.addEventListener('click', () => {
            mobileNavMenu.classList.add('active');
            mobileNavOverlay.classList.add('active');
        });

        closeMenuBtn.addEventListener('click', () => {
            mobileNavMenu.classList.remove('active');
            mobileNavOverlay.classList.remove('active');
        });

        mobileNavOverlay.addEventListener('click', () => {
            mobileNavMenu.classList.remove('active');
            mobileNavOverlay.classList.remove('active');
        });

        mobileNavLinks.forEach(link => {
            link.addEventListener('click', () => {
                mobileNavMenu.classList.remove('active');
                mobileNavOverlay.classList.remove('active');
            });
        });
    }
});

// Update Data (Centralized for both pages)
const updateData = {
    updateAnniversary: {
        image: "assets/Banner.png",
        headline: "Celebrating Our First Anniversary! 🥳",
        date: "June 03, 2026",
        writer: "Anjishnu Nandi",
        details: `
            <p>It's been an incredible year! We are thrilled to celebrate the first anniversary of Rhythm. What started as a vision for a professional, open-source music player has grown into a vibrant community of audiophiles.</p>
            
            <h3>🌟 A Year of Growth:</h3>
            <ul>
                <li>🚀 From simple local playback to the <strong>Streaming Revolution</strong> in v5.</li>
                <li>🎨 Evolving from basic themes to <strong>Material You Expressive</strong> design.</li>
                <li>🎧 Implementing studio-grade features like <strong>AutoEQ</strong> and <strong>High-Res Audio</strong>.</li>
                <li>🌍 Expanding to support users across the globe.</li>
            </ul>
            
            <p>We want to thank every single user, contributor, and supporter who helped us reach this milestone. Your feedback and passion drive us to keep pushing the boundaries of what a music player can be.</p>
            
            <p><strong>Here's to many more years of beautiful music and innovation! 🎵✨</strong></p>
        `
    },
    update51: {
        image: "assets/Posts/Rhythm_5.1_Release.png",
        headline: "Rhythm 5.1: The Refinement Update ✨",
        date: "June 14, 2026",
        writer: "Anjishnu Nandi",
        details: `
            <p>Rhythm 5.1 is here, bringing a massive wave of refinements, fixes, and new features based on your feedback. This release focuses on polishing the experience across all device form factors.</p>
            
            <h3>📱 Improved Tablet & Foldable Experience</h3>
            <p>The entire UI has been reworked for larger screens. Bottom sheets, player screens, library layouts, and navigation all adapt beautifully to tablets and foldables.</p>
            
            <h3>🆕 What's New</h3>
            <ul>
                <li>📜 <strong>A-Z Scroll Bar</strong> - Jump to any letter instantly in your library with the new expressive scroll bar.</li>
                <li>🎵 <strong>Rhythm Lyrics Widget</strong> - A brand-new Glance widget that displays synced lyrics right on your home screen.</li>
                <li>🔍 <strong>Nearby Server Discovery</strong> - Automatically discover Subsonic-compatible servers on your network.</li>
                <li>🎚️ <strong>Replay Gain</strong> - Experimental support for volume normalization across tracks.</li>
                <li>🖼️ <strong>Local Artist Images</strong> - Assign and display artist images from your local storage.</li>
            </ul>
            
            <h3>🐛 Bug Fixes</h3>
            <ul>
                <li>🔧 Equalizer unresponsiveness fixed (#411)</li>
                <li>📋 Playlists no longer disappear after restart (#414)</li>
                <li>🔀 Turning off shuffle no longer briefly stops playback (#287)</li>
                <li>💾 Backup & Restore now properly restores play counts (#419)</li>
                <li>🛡️ Whitelist scan mode now works reliably (#405)</li>
                <li>🖼️ Embedded album art displays correctly for WAV with ID3 tags (#409, #407)</li>
                <li>🔤 Song titles with apostrophes and periods render correctly (#401)</li>
                <li>🔁 Restored missing translation strings across all 24 languages</li>
            </ul>
            
            <h3>🌍 Translations</h3>
            <p>All 24 languages have been updated with the latest strings. AI-generated translations have been refreshed. We're preparing to move to Weblate for community-driven translations — stay tuned!</p>
            
            <p><strong>Download Rhythm 5.1 now from GitHub or your preferred source!</strong></p>
        `
    },
    update15: {
        image: "assets/Posts/Rhythm_5_Release.png",
        headline: "Rhythm 5: The Streaming Revolution 🌐",
        date: "June 03, 2026",
        writer: "Anjishnu Nandi",
        details: `
            <p>Rhythm 5 is here, and it's more than just an update—it's a complete evolution of how you experience music. We've introduced a <strong>Dual-Mode Architecture</strong>, allowing you to switch seamlessly between your local high-res library and a powerful new streaming ecosystem.</p>
            
            <h3>🚀 Key Highlights:</h3>
            <ul>
                <li>🌐 <strong>Streaming Ecosystem</strong> - Dedicated streaming providers, libraries, and a refined playback UI.</li>
                <li>🔍 <strong>Universal Search</strong> - A unified search experience for faster discovery across all your music sources.</li>
                <li>🎤 <strong>Full-Screen Lyrics</strong> - An immersive, karaoke-style view for deeper connection with your favorite tracks.</li>
                <li>🎨 <strong>Material Symbols</strong> - A modernized visual language for a cleaner, more intuitive interface.</li>
                <li>📊 <strong>Rhythm Stats 2.0</strong> - Deeper listening insights and a revamped UI to visualize your musical journey.</li>
            </ul>
            
            <h3>🎧 Audio & Performance:</h3>
            <ul>
                <li>🔊 <strong>Crossfade Support</strong> - Smooth transitions between tracks with new crossfade on skip.</li>
                <li>💎 <strong>ALAC Restored</strong> - High-resolution audio support is back and better than ever.</li>
                <li>⚡ <strong>Zero Lag</strong> - Optimized codebase that eliminates frame drops during playback and navigation.</li>
                <li>🛡️ <strong>Audio Stability</strong> - Fixed 16-bit crackling and concurrency issues for studio-grade stability.</li>
            </ul>
            
            <p><strong>Experience the revolution today!</strong> The latest stable version is available for download on our GitHub releases page.</p>
        `
    },
    update1: {
        image: "assets/update.jpg",
        headline: "Rhythm's Website Launch ;)",
        date: "August 27, 2025",
        writer: "Anjishnu Nandi",
        details: `
            <p>We're excited to announce the launch of our new website, designed to provide a better user experience and easier access to all things related to Rhythm.</p>
        `
    },
    update9: {
        image: "assets/Banner.png",
        headline: "Rhythm is Coming to Google Play Store! 📱🎉",
        date: "January 22, 2026",
        writer: "Anjishnu Nandi",
        details: `
            <p>🎊 Exciting news, Rhythm community! We're thrilled to announce that Rhythm will soon be available on the Google Play Store! This is a major milestone in our journey to bring professional music playback to Android users worldwide.</p>
            
            <h3>🚀 What's Coming to Google Play:</h3>
            <ul>
                <li>📱 <strong>Easy Installation</strong> - One-tap install from the Play Store</li>
                <li>🔄 <strong>Automatic Updates</strong> - Seamless updates through Google Play</li>
                <li>🌍 <strong>Global Availability</strong> - Available in all countries where Google Play operates</li>
                <li>🛡️ <strong>Enhanced Security</strong> - Additional security layers through Play Store verification</li>
                <li>📊 <strong>Usage Insights</strong> - Better analytics to improve the app (privacy-focused, of course!)</li>
            </ul>
            
            <h3>🔒 Privacy & Security First:</h3>
            <p>Your privacy remains our top priority. Rhythm has always been designed with privacy in mind, and our Google Play version maintains the same commitment:</p>
            <ul>
                <li>🚫 <strong>No Data Collection</strong> - We don't collect or share personal information</li>
                <li>🔐 <strong>Local Music Only</strong> - All your music stays on your device</li>
                <li>📋 <strong>Minimal Permissions</strong> - Only necessary permissions for music playback</li>
                <li>🛡️ <strong>Open Source</strong> - Full transparency with our GPL v3 license</li>
            </ul>
            
            <h3>⏰ Timeline:</h3>
            <p>We're currently in the final stages of Play Store preparation, including:</p>
            <ul>
                <li>✅ App review and compliance checks</li>
                <li>✅ Store listing optimization</li>
                <li>✅ Beta testing through Play Store</li>
                <li>🔄 Final approvals and publishing</li>
            </ul>
            
            <p><strong>Stay tuned for the official launch announcement!</strong> We'll keep you updated on our progress through our website and GitHub repository.</p>
            
            <h3>📥 Current Download Options:</h3>
            <p>While we prepare for Google Play, you can still download Rhythm from our GitHub releases for the latest features and updates. Our GitHub version will continue to receive updates alongside the Play Store version.</p>
            
            <p><strong>Thank you for your patience and continued support! 🎵✨</strong></p>
        `
    },
};



// Function to check if an image exists
function imageExists(url, callback) {
    const img = new Image();
    img.onload = function() { callback(true); };
    img.onerror = function() { callback(false); };
    img.src = url;
}

// News Carousel Functionality (for index.html)
function setupNewsCarousel() {
    const newsCarouselTrack = document.getElementById('dynamic-news-updates');
    const newsCarouselContainer = document.querySelector('.news-carousel');
    const newsPrevBtn = document.querySelector('.news-prev-btn');
    const newsNextBtn = document.querySelector('.news-next-btn');

    if (!newsCarouselTrack || !newsCarouselContainer || !newsPrevBtn || !newsNextBtn) {
        return; // Exit if news carousel elements are not found
    }

    // Clear existing content
    newsCarouselTrack.innerHTML = '';

    // Populate news items dynamically - sort by date (newest first)
    const sortedUpdateKeys = Object.keys(updateData).sort((a, b) => {
        const dateA = new Date(updateData[a].date);
        const dateB = new Date(updateData[b].date);
        return dateB - dateA; // Newest first
    });

    sortedUpdateKeys.forEach(key => {
        const data = updateData[key];
        const newsItem = document.createElement('div');
        newsItem.className = 'news-carousel-item';
        newsItem.setAttribute('data-update-id', key);

        let imageHtml = `<img src="${data.image || 'assets/Banner.png'}" alt="Update Image">`;
        if (!data.image) {
            newsItem.classList.add('no-image');
        }

        newsItem.innerHTML = `
            ${imageHtml}
            <div class="news-overlay">
                <h3>${data.headline}</h3>
                <p>${data.details.substring(0, 100)}...</p> <!-- Show a summary -->
                <a href="updates.html#${key}" class="btn btn-primary">Read More</a>
            </div>
        `;
        newsCarouselTrack.appendChild(newsItem);
    });

    const newsSlides = document.querySelectorAll('.news-carousel-item');
    let newsSlideIndex = 0;

    function getNewsSlidesPerView() {
        if (window.innerWidth >= 992) {
            return 3;
        } else if (window.innerWidth >= 768) {
            return 2;
        } else {
            return 1;
        }
    }

    function updateNewsCarousel() {
        const slidesPerView = getNewsSlidesPerView();
        const totalSlides = newsSlides.length;
        const slideWidth = newsCarouselContainer.offsetWidth / slidesPerView;

        newsSlides.forEach(slide => {
            slide.style.flex = `0 0 ${100 / slidesPerView}%`;
        });

        if (newsSlideIndex > totalSlides - slidesPerView && totalSlides >= slidesPerView) {
            newsSlideIndex = totalSlides - slidesPerView;
        } else if (newsSlideIndex < 0) {
            newsSlideIndex = 0;
        } else if (newsSlideIndex >= totalSlides) {
            newsSlideIndex = totalSlides - slidesPerView;
        }

        newsCarouselTrack.style.transform = `translateX(-${newsSlideIndex * slideWidth}px)`;
    }

    function showNewsSlide(index) {
        const slidesPerView = getNewsSlidesPerView();
        const totalSlides = newsSlides.length;

        newsSlideIndex = index;

        if (newsSlideIndex < 0) {
            newsSlideIndex = totalSlides - slidesPerView;
        } else if (newsSlideIndex > totalSlides - slidesPerView) {
            newsSlideIndex = 0;
        }
        updateNewsCarousel();
    }

    newsPrevBtn.addEventListener('click', () => {
        showNewsSlide(newsSlideIndex - 1);
    });

    newsNextBtn.addEventListener('click', () => {
        showNewsSlide(newsSlideIndex + 1);
    });

    window.addEventListener('resize', updateNewsCarousel);
    updateNewsCarousel(); // Initial update
    showNewsSlide(0); // Initialize to the first slide

    // Auto-scroll functionality for news carousel
    let newsAutoScrollInterval;
    function startNewsAutoScroll() {
        newsAutoScrollInterval = setInterval(() => {
            showNewsSlide(newsSlideIndex + 1);
        }, 5000); // Change slide every 5 seconds
    }

    function stopNewsAutoScroll() {
        clearInterval(newsAutoScrollInterval);
    }

    // Pause auto-scroll on hover
    newsCarouselContainer.addEventListener('mouseenter', stopNewsAutoScroll);
    newsCarouselContainer.addEventListener('mouseleave', startNewsAutoScroll);

    // Restart auto-scroll when manually navigating
    newsPrevBtn.addEventListener('click', () => {
        stopNewsAutoScroll();
        startNewsAutoScroll();
    });
    newsNextBtn.addEventListener('click', () => {
        stopNewsAutoScroll();
        startNewsAutoScroll();
    });

    startNewsAutoScroll(); // Start auto-scrolling on load
}

// Screenshot data
const phoneScreenshots = [
    { file: 'Home.png', label: 'Smart Home' },
    { file: 'Home2.png', label: 'Home Screen 2' },
    { file: 'Home3.png', label: 'Home Screen 3' },
    { file: 'Player.png', label: 'Now Playing' },
    { file: 'Player_2.png', label: 'Player Controls' },
    { file: 'Player_Lyrics_View.png', label: 'Synced Lyrics' },
    { file: 'Queue.png', label: 'Smart Queue' },
    { file: 'Search.png', label: 'Instant Search' },
    { file: 'Playlist.png', label: 'Playlists' },
    { file: 'Settings.png', label: 'Deep Settings' },
    { file: 'Artist.png', label: 'Artist Pages' },
    { file: 'Equalizer.png', label: '10-Band Equalizer' },
    { file: 'AutoEQ.png', label: 'AutoEQ Presets' },
    { file: 'Playback.png', label: 'Device Output' },
    { file: 'Sleep_Timer.png', label: 'Sleep Timer' },
    { file: 'Rhythm_Stats.png', label: 'Playback Stats' },
    { file: 'Tour.png', label: 'App Tour' },
    { file: 'Modes.png', label: 'App Modes' },
    { file: 'Shapes.png', label: 'Adaptive Shapes' },
    { file: 'Edit_Metadata.png', label: 'Metadata Editor' },
    { file: 'Song_Info.png', label: 'Song Information' },
    { file: 'Language_Switcher.png', label: 'Language Switcher' },
    { file: 'Multi-Selection.png', label: 'Multi Selection' },
    { file: 'Full_Screen_Lyrics_View.png', label: 'Full Screen Lyrics' },
    { file: 'About.png', label: 'About Rhythm' },
    { file: 'Updater.png', label: 'In-App Updater' },
];

const tabletScreenshots = [
    { file: 'Player.png', label: 'Now Playing' },
    { file: 'Player_Artist.png', label: 'Artist Now Playing' },
    { file: 'Lyrics_View.png', label: 'Synced Lyrics' },
    { file: 'Album.png', label: 'Album Detail' },
    { file: 'Library.png', label: 'Rich Library' },
    { file: 'Search.png', label: 'Instant Search' },
    { file: 'Rhythm_Stats.png', label: 'Playback Stats' },
    { file: 'Settings.png', label: 'Deep Settings' },
    { file: 'About.png', label: 'About Rhythm' },
    { file: 'Tour.png', label: 'App Tour' },
    { file: 'Tour_Step.png', label: 'Tour Detail' },
    { file: 'Updater.png', label: 'In-App Updater' },
];

let currentShowcaseView = 'phone';

function populateShowcaseTrack(view) {
    const track = document.getElementById('showcase-carousel-track');
    if (!track) return;
    currentShowcaseView = view;
    const data = view === 'phone' ? phoneScreenshots : tabletScreenshots;
    track.innerHTML = data.map(item =>
        `<div class="showcase-carousel-item">
            <img src="assets/ScreenShots/${view.charAt(0).toUpperCase() + view.slice(1)}/${item.file}" alt="${item.label}" loading="lazy">
        </div>`
    ).join('');
}

// Showcase Carousel Functionality (for index.html)
function setupShowcaseCarousel() {
    const showcaseCarouselTrack = document.getElementById('showcase-carousel-track');
    const showcaseCarouselContainer = document.querySelector('.showcase-carousel');
    const showcasePrevBtn = document.querySelector('.showcase-prev-btn');
    const showcaseNextBtn = document.querySelector('.showcase-next-btn');

    if (!showcaseCarouselTrack || !showcaseCarouselContainer || !showcasePrevBtn || !showcaseNextBtn) {
        return;
    }

    let showcaseSlideIndex = 0;

    function getShowcaseSlidesPerView() {
        if (window.innerWidth >= 992) return 3;
        if (window.innerWidth >= 768) return 2;
        return 1;
    }

    function updateShowcaseCarousel() {
        const slides = document.querySelectorAll('.showcase-carousel-item');
        const slidesPerView = getShowcaseSlidesPerView();
        const totalSlides = slides.length;
        const slideWidth = showcaseCarouselContainer.offsetWidth / slidesPerView;

        slides.forEach(slide => {
            slide.style.flex = `0 0 ${100 / slidesPerView}%`;
        });

        if (showcaseSlideIndex > totalSlides - slidesPerView && totalSlides >= slidesPerView) {
            showcaseSlideIndex = totalSlides - slidesPerView;
        } else if (showcaseSlideIndex < 0) {
            showcaseSlideIndex = 0;
        } else if (showcaseSlideIndex >= totalSlides) {
            showcaseSlideIndex = totalSlides - slidesPerView;
        }

        showcaseCarouselTrack.style.transform = `translateX(-${showcaseSlideIndex * slideWidth}px)`;
    }

    function showShowcaseSlide(index) {
        const slides = document.querySelectorAll('.showcase-carousel-item');
        const slidesPerView = getShowcaseSlidesPerView();
        const totalSlides = slides.length;

        showcaseSlideIndex = index;

        if (showcaseSlideIndex < 0) {
            showcaseSlideIndex = totalSlides - slidesPerView;
        } else if (showcaseSlideIndex > totalSlides - slidesPerView) {
            showcaseSlideIndex = 0;
        }
        updateShowcaseCarousel();
    }

    showcasePrevBtn.addEventListener('click', () => {
        showShowcaseSlide(showcaseSlideIndex - 1);
    });

    showcaseNextBtn.addEventListener('click', () => {
        showShowcaseSlide(showcaseSlideIndex + 1);
    });

    window.addEventListener('resize', updateShowcaseCarousel);
    updateShowcaseCarousel();
    showShowcaseSlide(0);

    let showcaseAutoScrollInterval;
    function startShowcaseAutoScroll() {
        stopShowcaseAutoScroll();
        showcaseAutoScrollInterval = setInterval(() => {
            showShowcaseSlide(showcaseSlideIndex + 1);
        }, 5000);
    }

    function stopShowcaseAutoScroll() {
        clearInterval(showcaseAutoScrollInterval);
    }

    showcaseCarouselContainer.addEventListener('mouseenter', stopShowcaseAutoScroll);
    showcaseCarouselContainer.addEventListener('mouseleave', startShowcaseAutoScroll);

    showcasePrevBtn.addEventListener('click', () => { stopShowcaseAutoScroll(); startShowcaseAutoScroll(); });
    showcaseNextBtn.addEventListener('click', () => { stopShowcaseAutoScroll(); startShowcaseAutoScroll(); });

    startShowcaseAutoScroll();
}

// View toggle
function setupViewToggle() {
    const toggleBtns = document.querySelectorAll('.view-toggle-btn');
    if (!toggleBtns.length) return;

    toggleBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            toggleBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            const view = btn.dataset.view;
            populateShowcaseTrack(view);
            // Reset and refresh carousel
            const showcaseCarouselTrack = document.getElementById('showcase-carousel-track');
            if (showcaseCarouselTrack) {
                showcaseCarouselTrack.style.transform = 'translateX(0)';
            }
            // Re-setup screenshot popup for new images
            if (window.showcaseAutoScrollInterval) {
                clearInterval(window.showcaseAutoScrollInterval);
            }
            // Re-bind popup click listeners
            rebindScreenshotPopup();
        });
    });
}

function rebindScreenshotPopup() {
    const slides = document.querySelectorAll('.showcase-carousel-item img');
    slides.forEach((slide, index) => {
        slide.addEventListener('click', () => {
            if (window.showScreenshotPopup) {
                window.showScreenshotPopup(index);
            }
        });
        slide.style.cursor = 'pointer';
    });
}

// Screenshot Popup Functionality (for index.html)
function setupScreenshotPopup() {
    const screenshotPopup = document.getElementById('screenshot-popup');
    const screenshotPopupImage = document.getElementById('screenshot-popup-image');
    const screenshotTitle = document.getElementById('screenshot-title');
    const screenshotCounter = document.getElementById('screenshot-counter');
    const closeScreenshotPopupBtn = document.getElementById('screenshot-close-btn');
    const zoomInBtn = document.getElementById('zoom-in-btn');
    const zoomOutBtn = document.getElementById('zoom-out-btn');
    const zoomResetBtn = document.getElementById('zoom-reset-btn');
    const zoomLevelDisplay = document.getElementById('zoom-level');
    const screenshotPrevBtn = document.getElementById('screenshot-prev-btn');
    const screenshotNextBtn = document.getElementById('screenshot-next-btn');

    if (!screenshotPopup || !screenshotPopupImage || !closeScreenshotPopupBtn) {
        return; // Exit if screenshot popup elements are not found
    }

    // Get all showcase carousel items
    let currentScreenshotIndex = 0;
    let currentZoom = 1;
    const minZoom = 0.5;
    const maxZoom = 3.0;
    const zoomStep = 0.25;
    let isPanning = false;
    let startX, startY, initialX, initialY;

    // Function to get screenshot title from alt text
    function getScreenshotTitle(imgElement) {
        return imgElement.alt || 'Screenshot';
    }

    // Function to update zoom level display
    function updateZoomDisplay() {
        const percentage = Math.round(currentZoom * 100);
        zoomLevelDisplay.textContent = `${percentage}%`;
    }

    // Function to apply zoom to image
    function applyZoom(zoomValue) {
        screenshotPopupImage.style.transform = `scale(${zoomValue})`;
        updateZoomDisplay();
    }

    // Function to zoom in
    function zoomIn() {
        if (currentZoom < maxZoom) {
            currentZoom = Math.min(currentZoom * 1.5, maxZoom);
            applyZoom(currentZoom);
        }
    }

    // Function to zoom out
    function zoomOut() {
        if (currentZoom > minZoom) {
            currentZoom = Math.max(currentZoom * 0.75, minZoom);
            applyZoom(currentZoom);
        }
    }

    // Function to reset zoom and position
    function resetZoom() {
        currentZoom = 1;
        screenshotPopupImage.style.transform = 'scale(1) translate(0, 0)';
        updateZoomDisplay();
    }

    // Function to get current slides
    function getCurrentSlides() {
        return document.querySelectorAll('.showcase-carousel-item img');
    }

    // Function to show screenshot popup
    function showScreenshotPopup(index) {
        const slides = getCurrentSlides();
        if (index >= 0 && index < slides.length) {
            currentScreenshotIndex = index;
            const imgElement = slides[index];
            const imgSrc = imgElement.src;
            const imgAlt = imgElement.alt;
            const title = getScreenshotTitle(imgElement);

            // Update popup content
            screenshotPopupImage.src = imgSrc;
            screenshotPopupImage.alt = imgAlt;
            screenshotTitle.textContent = title;
            screenshotCounter.textContent = `${index + 1} of ${slides.length}`;

            // Reset zoom and enable/disable navigation buttons
            resetZoom();
            screenshotPrevBtn.disabled = currentScreenshotIndex === 0;
            screenshotNextBtn.disabled = currentScreenshotIndex === slides.length - 1;

            // Show popup
            screenshotPopup.classList.add('active');

            // Pause showcase carousel auto-scroll
            if (typeof window.showcaseAutoScrollInterval !== 'undefined') {
                clearInterval(window.showcaseAutoScrollInterval);
            }
        }
    }

    // Function to hide screenshot popup
    function hideScreenshotPopup() {
        screenshotPopup.classList.remove('active');
        resetZoom();
    }

    // Add click event listeners to showcase images
    rebindScreenshotPopup();

    // Close popup event listeners
    closeScreenshotPopupBtn.addEventListener('click', hideScreenshotPopup);
    screenshotPopup.addEventListener('click', (e) => {
        if (e.target === screenshotPopup) {
            hideScreenshotPopup();
        }
    });

    // Zoom controls
    zoomInBtn.addEventListener('click', zoomIn);
    zoomOutBtn.addEventListener('click', zoomOut);
    zoomResetBtn.addEventListener('click', resetZoom);

    // Mouse wheel zoom
    screenshotPopupImage.addEventListener('wheel', (e) => {
        e.preventDefault();
        if (e.deltaY < 0) {
            zoomIn();
        } else {
            zoomOut();
        }
    });

    // Pan functionality (drag to move zoomed image)
    screenshotPopupImage.addEventListener('mousedown', (e) => {
        if (currentZoom > 1) {
            isPanning = true;
            startX = e.clientX;
            startY = e.clientY;
            const transform = getComputedStyle(screenshotPopupImage).transform;
            const matrix = new DOMMatrix(transform);
            initialX = matrix.m41 || 0;
            initialY = matrix.m42 || 0;
            screenshotPopupImage.style.cursor = 'grabbing';
        }
    });

    document.addEventListener('mousemove', (e) => {
        if (isPanning && currentZoom > 1) {
            const dx = e.clientX - startX;
            const dy = e.clientY - startY;
            const newX = initialX + dx;
            const newY = initialY + dy;
            screenshotPopupImage.style.transform = `scale(${currentZoom}) translate(${newX}px, ${newY}px)`;
        }
    });

    document.addEventListener('mouseup', () => {
        isPanning = false;
        screenshotPopupImage.style.cursor = currentZoom > 1 ? 'grab' : 'default';
    });

    // Double-click to reset zoom
    screenshotPopupImage.addEventListener('dblclick', resetZoom);

    // Navigation buttons event listeners
    screenshotPrevBtn.addEventListener('click', () => {
        if (currentScreenshotIndex > 0) {
            showScreenshotPopup(currentScreenshotIndex - 1);
        }
    });

    screenshotNextBtn.addEventListener('click', () => {
        const slides = getCurrentSlides();
        if (currentScreenshotIndex < slides.length - 1) {
            showScreenshotPopup(currentScreenshotIndex + 1);
        }
    });

    // Keyboard navigation
    document.addEventListener('keydown', (e) => {
        if (!screenshotPopup.classList.contains('active')) return;

        switch(e.key) {
            case 'ArrowLeft':
                if (e.ctrlKey || e.metaKey) {
                    zoomOut();
                } else {
                    screenshotPrevBtn.click();
                }
                break;
            case 'ArrowRight':
                if (e.ctrlKey || e.metaKey) {
                    zoomIn();
                } else {
                    screenshotNextBtn.click();
                }
                break;
            case 'ArrowUp':
                zoomIn();
                break;
            case 'ArrowDown':
                zoomOut();
                break;
            case '0':
            case 'Home':
                resetZoom();
                break;
            case 'Escape':
                hideScreenshotPopup();
                break;
        }
    });

    window.showScreenshotPopup = showScreenshotPopup; // Make it globally accessible
}

// Update Popup Functionality (for updates.html)
function setupUpdatePopup() {
    const updateItems = document.querySelectorAll('.updates-list .update-item');
    const updatePopup = document.getElementById('update-popup');
    const closePopupBtn = document.querySelector('.close-popup-btn');
    const popupImage = document.getElementById('popup-image');
    const popupHeadline = document.getElementById('popup-headline');
    const popupDate = document.getElementById('popup-date');
    const popupWriter = document.getElementById('popup-writer');
    const popupDetails = document.getElementById('popup-details');

    if (!updatePopup) return; // Exit if not on updates.html

    updateItems.forEach(item => {
        const updateId = item.getAttribute('data-update-id');
        const data = updateData[updateId];
        const itemImage = item.querySelector('img');

        if (data && data.image) {
            imageExists(`${data.image}`, (exists) => { // Path is relative to updates.html
                if (!exists) {
                    item.classList.add('no-image');
                    itemImage.insertAdjacentHTML('afterend', `<img src="assets/icon.png" alt="Rhythm Logo" class="fallback-logo">`);
                }
            });
        } else {
            item.classList.add('no-image');
            itemImage.insertAdjacentHTML('afterend', `<img src="assets/icon.png" alt="Rhythm Logo" class="fallback-logo">`);
        }

        item.addEventListener('click', () => {
            if (data) {
                if (data.image) {
                    imageExists(`${data.image}`, (exists) => { // Path is relative to updates.html
                        if (exists) {
                            popupImage.src = `${data.image}`;
                        } else {
                            popupImage.src = "assets/icon.png"; // Fallback to app logo
                        }
                    });
                } else {
                    popupImage.src = "assets/icon.png"; // Fallback to app logo
                }
                
                popupHeadline.textContent = data.headline;
                popupDate.textContent = data.date;
                popupWriter.textContent = data.writer;
                popupDetails.innerHTML = data.details;
                updatePopup.classList.add('active');
            }
        });
    });

    closePopupBtn.addEventListener('click', () => {
        updatePopup.classList.remove('active');
    });

    updatePopup.addEventListener('click', (e) => {
        if (e.target === updatePopup) {
            updatePopup.classList.remove('active');
        }
    });
}


// Smooth scrolling for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();

        const targetId = this.getAttribute('href');
        if (targetId === '#') {
            // For the logo, scroll to the absolute top
            window.scrollTo({
                top: 0,
                behavior: 'smooth'
            });
        } else {
            const target = document.querySelector(targetId);
            if (target) {
                window.scrollTo({
                    top: target.offsetTop - 80,
                    behavior: 'smooth'
                });
            }
        }
    });
});


// Animation on scroll
function setupScrollAnimations() {
    const animateElements = document.querySelectorAll('.feature-card, .section-header, .dashboard-preview');

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.animation = 'fadeIn 0.6s forwards';
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });

    animateElements.forEach(element => {
        element.style.opacity = '0';
        observer.observe(element);
    });
}

// Dark mode toggle
function setupDarkModeToggle() {
    const footerSection = document.querySelector('.footer-section:first-child');
    const darkModeToggle = document.createElement('div');
    darkModeToggle.className = 'dark-mode-toggle';
    darkModeToggle.innerHTML = `
        <label class="switch">
            <input type="checkbox" id="darkModeSwitch">
            <span class="slider"></span>
        </label>
        <span>Dark Mode</span>
    `;

    // Insert styles for the toggle
    const styleEl = document.createElement('style');
    styleEl.textContent = `
        .dark-mode-toggle {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-top: 20px;
        }
        .switch {
            position: relative;
            display: inline-block;
            width: 60px;
            height: 30px;
        }
        .switch input {
            opacity: 0;
            width: 0;
            height: 0;
        }
        .slider {
            position: absolute;
            cursor: pointer;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background-color: #ccc;
            transition: .4s;
            border-radius: 34px;
        }
        .slider:before {
            position: absolute;
            content: "";
            height: 22px;
            width: 22px;
            left: 4px;
            bottom: 4px;
            background-color: white;
            transition: .4s;
            border-radius: 50%;
        }
        input:checked + .slider {
            background-color: var(--primary); /* Ensure dark mode toggle uses primary color */
        }
        input:checked + .slider:before {
            transform: translateX(30px);
        }
    `;
    document.head.appendChild(styleEl);

    footerSection.appendChild(darkModeToggle);

    const darkModeSwitch = document.getElementById('darkModeSwitch');

    // Function to apply theme
    function applyTheme(isDark) {
        if (isDark) {
            document.body.classList.add('dark-mode');
        } else {
            document.body.classList.remove('dark-mode');
        }
    }

    // Check for saved preference first
    const savedTheme = localStorage.getItem('darkMode');
    if (savedTheme === 'enabled') {
        applyTheme(true);
        darkModeSwitch.checked = true;
    } else if (savedTheme === 'disabled') {
        applyTheme(false);
        darkModeSwitch.checked = false;
    } else {
        // If no saved preference, check system preference
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        applyTheme(prefersDark);
        darkModeSwitch.checked = prefersDark;
    }

    // Listen for changes in system theme
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
        // Only apply system theme if no explicit user preference is set
        if (localStorage.getItem('darkMode') === null) {
            applyTheme(e.matches);
            darkModeSwitch.checked = e.matches;
        }
    });

    // Dark mode toggle event listener
    darkModeSwitch.addEventListener('change', () => {
        if (darkModeSwitch.checked) {
            applyTheme(true);
            localStorage.setItem('darkMode', 'enabled');
        } else {
            applyTheme(false);
            localStorage.setItem('darkMode', 'disabled'); // Save 'disabled' to explicitly turn off dark mode
        }
    });
}

// Preloader
function setupPreloader() {
    const preloader = document.createElement('div');
    preloader.className = 'preloader';
    preloader.innerHTML = `
        <div class="spinner">
            <i class="fa-solid fa-arrows-rotate fa-spin"></i>
        </div>
    `;

    // Insert styles for the preloader
    const styleEl = document.createElement('style');
    styleEl.textContent = `
        .preloader {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background-color: var(--light);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 9999;
            transition: opacity 0.5s ease-out, visibility 0.5s ease-out;
        }
        .spinner {
            font-size: 80px;
            color: var(--primary);
            // animation: pulse 1.5s infinite;
        }
        @keyframes pulse {
            0% { transform: scale(0.9); opacity: 0.7; }
            50% { transform: scale(1.1); opacity: 1; }
            100% { transform: scale(0.9); opacity: 0.7; }
        }
        .preloader.hidden {
            opacity: 0;
            visibility: hidden;
        }
    `;
    document.head.appendChild(styleEl);

    document.body.prepend(preloader);

    window.addEventListener('load', () => {
        setTimeout(() => {
            preloader.classList.add('hidden');
            requestAnimationFrame(() => {
                requestAnimationFrame(() => {
                    document.body.classList.add('loaded');
                });
            });
        }, 300);
    });
}

// Dynamic Material 3 Color Generator
function setupDynamicColors() {
    var SEED_KEY = 'rhythm_m3_seed';
    var seedHue = sessionStorage.getItem(SEED_KEY);
    if (!seedHue) {
        seedHue = Math.floor(Math.random() * 360);
        sessionStorage.setItem(SEED_KEY, seedHue);
    } else {
        seedHue = parseInt(seedHue, 10);
    }

    function hslToHex(h, s, l) {
        var H = h / 360, S = s / 100, L = l / 100;
        var a = S * Math.min(L, 1 - L);
        function f(n) {
            var k = (n + H * 12) % 12;
            var color = L - a * Math.max(-1, Math.min(k - 3, 9 - k, 1));
            return Math.round(255 * color).toString(16).padStart(2, '0');
        }
        return '#' + f(0) + f(8) + f(4);
    }

    function hexToRgb(hex) {
        var r = parseInt(hex.slice(1, 3), 16);
        var g = parseInt(hex.slice(3, 5), 16);
        var b = parseInt(hex.slice(5, 7), 16);
        return r + ', ' + g + ', ' + b;
    }

    function hslStr(h, s, l) { return Math.round(h) + ', ' + Math.round(s) + '%, ' + Math.round(l) + '%'; }

    function generatePalette(hue, dark) {
        var H = hue, H2 = (hue + 30) % 360, H3 = (hue + 60) % 360;
        if (dark) {
            return {
                '--primary': hslToHex(H, 50, 80),
                '--on-primary': hslToHex(H, 70, 18),
                '--primary-container': hslToHex(H, 50, 28),
                '--on-primary-container': hslToHex(H, 40, 90),
                '--secondary': hslToHex(H2, 40, 75),
                '--on-secondary': hslToHex(H2, 50, 18),
                '--secondary-container': hslToHex(H2, 40, 28),
                '--on-secondary-container': hslToHex(H2, 35, 90),
                '--tertiary': hslToHex(H3, 40, 75),
                '--on-tertiary': hslToHex(H3, 50, 18),
                '--tertiary-container': hslToHex(H3, 40, 28),
                '--on-tertiary-container': hslToHex(H3, 35, 90),
                '--error': '#F2B8B5', '--on-error': '#601410',
                '--error-container': '#8C1D18', '--on-error-container': '#F9DEDC',
                '--background': hslToHex(H, 6, 6),
                '--on-background': hslToHex(H, 4, 90),
                '--surface': hslToHex(H, 6, 6),
                '--on-surface': hslToHex(H, 4, 90),
                '--surface-variant': hslToHex(H2, 8, 28),
                '--on-surface-variant': hslToHex(H2, 8, 80),
                '--outline': hslToHex(H2, 10, 60),
                '--outline-variant': hslToHex(H2, 8, 38),
                '--shadow': '#000000',
                '--inverse-surface': hslToHex(H, 4, 90),
                '--inverse-on-surface': hslToHex(H, 6, 18),
                '--inverse-primary': hslToHex(H, 60, 40),
                '--surface-tint': hslToHex(H, 50, 80),
                '--ripple': 'rgba(' + hexToRgb(hslToHex(H, 50, 80)) + ', 0.12)',
                '--surface-hsl': hslStr(H, 6, 6),
                '--surface-container-lowest': hslToHex(H, 5, 4),
                '--surface-container-low': hslToHex(H, 6, 8),
                '--surface-container': hslToHex(H, 6, 12),
                '--surface-container-high': hslToHex(H, 6, 16),
                '--surface-container-highest': hslToHex(H, 6, 20),
                '--primary-light': hslToHex(H, 50, 28),
                '--primary-dark': hslToHex(H, 70, 18),
                '--on-primary-dark': hslToHex(H, 50, 80),
                '--accent': hslToHex(H2, 40, 75),
                '--dark': hslToHex(H, 4, 90),
                '--dark-light': hslToHex(H2, 8, 80),
                '--light': hslToHex(H, 6, 6),
                '--text-dark': hslToHex(H, 4, 90),
                '--text-light': hslToHex(H, 70, 18),
                '--success': '#06d6a0', '--warning': '#ffbe0b', '--danger': '#ef476f',
                '--gray': hslToHex(H2, 8, 28),
                '--gray-dark': hslToHex(H2, 10, 60)
            };
        }
        return {
            '--primary': hslToHex(H, 60, 40),
            '--on-primary': '#FFFFFF',
            '--primary-container': hslToHex(H, 50, 90),
            '--on-primary-container': hslToHex(H, 70, 10),
            '--secondary': hslToHex(H2, 45, 50),
            '--on-secondary': '#FFFFFF',
            '--secondary-container': hslToHex(H2, 40, 90),
            '--on-secondary-container': hslToHex(H2, 55, 10),
            '--tertiary': hslToHex(H3, 45, 50),
            '--on-tertiary': '#FFFFFF',
            '--tertiary-container': hslToHex(H3, 40, 90),
            '--on-tertiary-container': hslToHex(H3, 55, 10),
            '--error': '#B3261E', '--on-error': '#FFFFFF',
            '--error-container': '#F9DEDC', '--on-error-container': '#410E0B',
            '--background': hslToHex(H, 8, 94),
            '--on-background': hslToHex(H, 10, 10),
            '--surface': hslToHex(H, 6, 98),
            '--on-surface': hslToHex(H, 10, 10),
            '--surface-variant': hslToHex(H2, 10, 90),
            '--on-surface-variant': hslToHex(H2, 10, 30),
            '--outline': hslToHex(H2, 12, 50),
            '--outline-variant': hslToHex(H2, 10, 80),
            '--shadow': '#000000',
            '--inverse-surface': hslToHex(H, 6, 18),
            '--inverse-on-surface': hslToHex(H, 4, 95),
            '--inverse-primary': hslToHex(H, 50, 80),
            '--surface-tint': hslToHex(H, 60, 40),
            '--ripple': 'rgba(' + hexToRgb(hslToHex(H, 60, 40)) + ', 0.12)',
            '--surface-hsl': hslStr(H, 6, 98),
            '--surface-container-lowest': hslToHex(H, 5, 100),
            '--surface-container-low': hslToHex(H, 6, 96),
            '--surface-container': hslToHex(H, 6, 94),
            '--surface-container-high': hslToHex(H, 6, 92),
            '--surface-container-highest': hslToHex(H, 6, 90),
            '--primary-light': hslToHex(H, 50, 90),
            '--primary-dark': hslToHex(H, 70, 30),
            '--on-primary-dark': hslToHex(H, 40, 85),
            '--accent': hslToHex(H2, 45, 50),
            '--dark': hslToHex(H, 10, 10),
            '--dark-light': hslToHex(H2, 10, 30),
            '--light': hslToHex(H, 8, 94),
            '--text-dark': hslToHex(H, 10, 10),
            '--text-light': '#FFFFFF',
            '--success': '#06d6a0', '--warning': '#ffbe0b', '--danger': '#ef476f',
            '--gray': hslToHex(H2, 10, 90),
            '--gray-dark': hslToHex(H2, 12, 50)
        };
    }

    var lightColors = generatePalette(seedHue, false);
    var darkColors = generatePalette(seedHue, true);

    function applyThemeColors(isDark) {
        var colors = isDark ? darkColors : lightColors;
        var root = document.documentElement;
        for (var key in colors) {
            if (colors.hasOwnProperty(key)) {
                root.style.setProperty(key, colors[key]);
            }
        }
    }

    applyThemeColors(document.body.classList.contains('dark-mode'));

    var darkModeSwitch = document.getElementById('darkModeSwitch');
    if (darkModeSwitch) {
        darkModeSwitch.addEventListener('change', function () {
            applyThemeColors(this.checked);
        });
    }
}

// Initialize all functionality
document.addEventListener('DOMContentLoaded', () => {
    setupScrollAnimations();
    setupDarkModeToggle();
    setupDynamicColors();
    setupPreloader();

    const currentPage = window.location.pathname.split('/').pop();

    if (currentPage === 'index.html' || currentPage === '') {
        // Auto-detect view: tablet on desktop, phone on mobile
        const initialView = window.innerWidth >= 992 ? 'tablet' : 'phone';
        const activeBtn = document.querySelector(`.view-toggle-btn[data-view="${initialView}"]`);
        if (activeBtn) {
            document.querySelectorAll('.view-toggle-btn').forEach(b => b.classList.remove('active'));
            activeBtn.classList.add('active');
        }
        populateShowcaseTrack(initialView);
        setupNewsCarousel(); // Initialize news carousel functionality for index.html
        setupShowcaseCarousel(); // Initialize showcase carousel functionality for index.html
        setupScreenshotPopup(); // Initialize screenshot popup functionality for index.html
        setupViewToggle(); // Initialize view toggle
    } else if (currentPage === 'updates.html') {
        setupUpdatePopup(); // Initialize update popup functionality for updates.html
        setupUpdateViewToggle(); // Initialize update view toggle functionality for updates.html
    }
});

// Update View Toggle Functionality (for updates.html)
function setupUpdateViewToggle() {
    const listViewBtn = document.getElementById('listViewBtn');
    const gridViewBtn = document.getElementById('gridViewBtn');
    const updatesList = document.querySelector('.updates-list');

    if (!listViewBtn || !gridViewBtn || !updatesList) {
        return; // Exit if elements are not found
    }

    // Set default view to list view
    updatesList.classList.add('list-view');
    listViewBtn.classList.add('btn-primary', 'active');
    gridViewBtn.classList.remove('btn-primary', 'active');
    gridViewBtn.classList.add('btn-outline');

    listViewBtn.addEventListener('click', () => {
        updatesList.classList.remove('grid-view');
        updatesList.classList.add('list-view');
        listViewBtn.classList.add('btn-primary', 'active');
        listViewBtn.classList.remove('btn-outline');
        gridViewBtn.classList.remove('btn-primary', 'active');
        gridViewBtn.classList.add('btn-outline');
    });

    gridViewBtn.addEventListener('click', () => {
        updatesList.classList.remove('list-view');
        updatesList.classList.add('grid-view');
        gridViewBtn.classList.add('btn-primary', 'active');
        gridViewBtn.classList.remove('btn-outline');
        listViewBtn.classList.remove('btn-primary', 'active');
        listViewBtn.classList.add('btn-outline');
    });
}

// Smooth scrolling for all download buttons
document.querySelectorAll('.scroll-to-download').forEach(button => {
    button.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            window.scrollTo({
                top: target.offsetTop - 80, // Adjust for fixed header
                behavior: 'smooth'
            });
        }
    });
});

// Help Page Functionality
document.addEventListener('DOMContentLoaded', () => {
    const helpContent = document.querySelector('.help-content');
    const searchInput = document.getElementById('helpSearch');
    const sidebarLinks = document.querySelectorAll('.help-sidebar-link');
    const sections = document.querySelectorAll('.help-section-block');
    const header = document.querySelector('header');
    const headerHeight = header ? header.offsetHeight : 80;

    // Build mobile sidebar
    const sidebar = document.getElementById('helpSidebar');
    if (sidebar) {
        const mobileNav = document.createElement('div');
        mobileNav.className = 'mobile-sidebar';
        const navClone = sidebar.querySelector('nav').cloneNode(true);
        navClone.querySelectorAll('a').forEach(a => a.className = 'mobile-sidebar-link');
        mobileNav.appendChild(navClone);
        helpContent.parentElement.insertBefore(mobileNav, helpContent);
    }

    // Search filter
    if (searchInput) {
        searchInput.addEventListener('input', () => {
            const term = searchInput.value.toLowerCase().trim();
            document.querySelectorAll('.help-card').forEach(card => {
                const match = term === '' || card.textContent.toLowerCase().includes(term);
                card.style.display = match ? '' : 'none';
            });
            document.querySelectorAll('.help-section-block').forEach(section => {
                const visible = term === '' || Array.from(section.querySelectorAll('.help-card')).some(c => c.style.display !== 'none');
                section.style.display = visible ? '' : 'none';
            });
        });
    }

    // Scroll spy
    function updateActiveLink() {
        let current = '';
        const scrollY = window.scrollY + headerHeight + 20;
        sections.forEach(s => {
            if (s.offsetTop <= scrollY) current = s.id;
        });
        document.querySelectorAll('.help-sidebar-link, .mobile-sidebar-link').forEach(link => {
            link.classList.toggle('active', link.dataset.target === current);
        });
    }

    window.addEventListener('scroll', updateActiveLink);
    updateActiveLink();

    // Smooth scroll on sidebar click
    document.querySelectorAll('.help-sidebar-link, .mobile-sidebar-link').forEach(link => {
        link.addEventListener('click', e => {
            e.preventDefault();
            const target = document.getElementById(link.dataset.target);
            if (target) {
                window.scrollTo({ top: target.offsetTop - headerHeight - 10, behavior: 'smooth' });
            }
        });
    });
});

// Back to Top Button
document.addEventListener('DOMContentLoaded', () => {
    // Create back to top button
    const backToTopBtn = document.createElement('button');
    backToTopBtn.className = 'back-to-top';
    backToTopBtn.innerHTML = '<i class="fas fa-arrow-up"></i>';
    backToTopBtn.setAttribute('aria-label', 'Back to top');
    document.body.appendChild(backToTopBtn);

    // Show/hide back to top button
    window.addEventListener('scroll', () => {
        if (window.scrollY > 300) {
            backToTopBtn.classList.add('show');
        } else {
            backToTopBtn.classList.remove('show');
        }
    });

    // Scroll to top when clicked
    backToTopBtn.addEventListener('click', () => {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
    });

    // Reading progress indicator for help page
    if (document.querySelector('.help-layout')) {
        const progressBar = document.createElement('div');
        progressBar.className = 'reading-progress';
        document.body.appendChild(progressBar);

        window.addEventListener('scroll', () => {
            const scrollTop = window.scrollY;
            const docHeight = document.documentElement.scrollHeight - window.innerHeight;
            const scrollPercent = (scrollTop / docHeight) * 100;
            progressBar.style.width = scrollPercent + '%';
        });
    }
});
