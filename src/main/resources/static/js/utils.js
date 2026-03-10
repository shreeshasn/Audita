/* =============================================
   AUDITA — utils.js
   Shared constants, helpers, and the global
   apiFetch wrapper (cold-start awareness)
   ============================================= */

// Locally Spring runs on 8081, on Render everything is same-origin
const API_BASE = window.location.hostname === 'localhost'
  ? 'http://localhost:8081'
  : '';

const CATEGORY_ICONS = {
  'Documentation':   '📄',
  'CI/CD':           '⚙️',
  'Docker':          '🐳',
  'Code Quality':    '🧪',
  'Community':       '🌍',
  'Workflow Quality':'🔁',
  'Release Cadence': '🚀'
};

// ── Shared helpers ────────────────────────────────────────────────────────────

function cleanGithubUrl(url) {
  const match = url.match(/https?:\/\/github\.com\/([^\/]+)\/([^\/\?#]+)/);
  if (!match) return url;
  return `https://github.com/${match[1]}/${match[2]}`;
}

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

function formatNum(n) {
  if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M';
  if (n >= 1000)    return (n / 1000).toFixed(1) + 'k';
  return n?.toString() || '0';
}

function hexToRgb(hex) {
  const r = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!r) return '0,255,136';
  return `${parseInt(r[1],16)},${parseInt(r[2],16)},${parseInt(r[3],16)}`;
}

// ── Global waking pill ────────────────────────────────────────────────────────
// If any apiFetch call takes longer than WAKE_THRESHOLD ms, show the pill.
// It hides automatically when the response returns.
// Multiple simultaneous calls are reference-counted — pill stays until all done.

const WAKE_THRESHOLD = 5000; // ms before showing "Waking server..."
let _activeRequests = 0;
let _wakeTimer = null;
let _wakeEscalateTimer = null;

function _showPill(msg) {
  const pill = document.getElementById('wakingPill');
  const text = document.getElementById('wakingPillText');
  if (!pill) return;
  if (text && msg) text.textContent = msg;
  pill.classList.add('visible');
}

function _hidePill() {
  const pill = document.getElementById('wakingPill');
  if (!pill) return;
  pill.classList.remove('visible');
}

function _onRequestStart() {
  _activeRequests++;
  if (_activeRequests === 1) {
    // After 5s: first message
    _wakeTimer = setTimeout(() => {
      _showPill('Waking server...');
      // After another 15s (20s total): escalate
      _wakeEscalateTimer = setTimeout(() =>
        _showPill('Still starting up — first load can take ~50s'), 15000);
    }, WAKE_THRESHOLD);
  }
}

function _onRequestEnd() {
  _activeRequests = Math.max(0, _activeRequests - 1);
  if (_activeRequests === 0) {
    clearTimeout(_wakeTimer);
    clearTimeout(_wakeEscalateTimer);
    _hidePill();
  }
}

/**
 * apiFetch — drop-in fetch() replacement for all API calls.
 * Same signature: apiFetch(url) returns a Response promise.
 * Automatically shows/hides the waking pill if the server is slow.
 */
async function apiFetch(url) {
  _onRequestStart();
  try {
    const res = await fetch(url);
    return res;
  } finally {
    _onRequestEnd();
  }
}