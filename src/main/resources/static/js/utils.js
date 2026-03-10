/* =============================================
   AUDITA — utils.js
   Shared constants and utility functions
   used by both index.js and compare.js
   ============================================= */

const API_BASE = 'http://localhost:8081';

const CATEGORY_ICONS = {
  'Documentation': '📄',
  'CI/CD': '⚙️',
  'Docker': '🐳',
  'Code Quality': '🧪',
  'Community': '🌍',
  'Workflow Quality': '🔁',
  'Release Cadence': '🚀'
};

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
  return `${parseInt(r[1], 16)},${parseInt(r[2], 16)},${parseInt(r[3], 16)}`;
}