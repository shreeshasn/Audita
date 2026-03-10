/* ── index.js — depends on utils.js ── */

const CIRCUMFERENCE = 2 * Math.PI * 85;

const SUGGESTION_LINKS = {
  'README':        'https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes',
  'LICENSE':       'https://choosealicense.com',
  'CONTRIBUTING':  'https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions/setting-guidelines-for-repository-contributors',
  'CHANGELOG':     'https://keepachangelog.com',
  'GitHub Actions':'https://docs.github.com/en/actions/quickstart',
  '.github':       'https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions/creating-a-default-community-health-file',
  'Dockerfile':    'https://docs.docker.com/get-started/02_our_app/',
  'docker-compose':'https://docs.docker.com/compose/gettingstarted/',
  '.gitignore':    'https://git-scm.com/docs/gitignore',
  'linting':       'https://eslint.org/docs/latest/use/getting-started',
  'SECURITY':      'https://docs.github.com/en/code-security/getting-started/adding-a-security-policy-to-your-repository',
  'topics':        'https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/classifying-your-repository-with-topics',
  'caching':       'https://docs.github.com/en/actions/writing-workflows/choosing-what-your-workflow-does/caching-dependencies-to-speed-up-workflows',
  'pull_request':  'https://docs.github.com/en/actions/writing-workflows/choosing-when-your-workflow-runs/events-that-trigger-workflows',
  'timeout':       'https://docs.github.com/en/actions/writing-workflows/workflow-syntax-for-github-actions#jobsjob_idtimeout-minutes',
  'pinned':        'https://docs.github.com/en/actions/security-for-github-actions/security-guides/security-hardening-for-github-actions',
  'Releases':      'https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository',
  'description':   'https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes'
};

const LANG_COLORS = ['#00ff88','#0088ff','#ff6b35','#ffcc00','#ff4466','#a855f7','#06b6d4','#f59e0b','#10b981','#6366f1'];
const GRADE_COLORS = { A:'#00ff88', B:'#00cc88', C:'#ffcc00', D:'#ff4466' };
const GRADE_LABELS = { A:'Excellent', B:'Good', C:'Needs Work', D:'Poor' };

let currentData = null;
let currentRepoUrl = null;

// ── Helpers ──────────────────────────────────────────────────────────────────

function getSuggestionLink(text) {
  for (const [k, url] of Object.entries(SUGGESTION_LINKS))
    if (text.toLowerCase().includes(k.toLowerCase())) return url;
  return null;
}

function timeAgo(d) {
  if (!d) return '—';
  const days = Math.floor((Date.now() - new Date(d)) / 86400000);
  if (days === 0) return 'today';
  if (days === 1) return '1d ago';
  if (days < 30)  return `${days}d ago`;
  if (days < 365) return `${Math.floor(days / 30)}mo ago`;
  return `${Math.floor(days / 365)}y ago`;
}

function getFileIcon(f) {
  if (f.endsWith('.md'))  return '📝';
  if (f.endsWith('.json')) return '{}';
  if (f.endsWith('.yml') || f.endsWith('.yaml')) return '⚙️';
  if (f.startsWith('.'))  return '🔧';
  if (f.toLowerCase().includes('docker')) return '🐳';
  return '📄';
}

function switchTab(name) {
  document.querySelectorAll('.tab-btn').forEach((btn, i) =>
    btn.classList.toggle('active', (i === 0 && name === 'audit') || (i === 1 && name === 'analytics'))
  );
  document.getElementById('tab-audit').classList.toggle('active', name === 'audit');
  document.getElementById('tab-analytics').classList.toggle('active', name === 'analytics');
}

// ── Analysis flow ─────────────────────────────────────────────────────────────

async function startAnalysis() {
  const input = document.getElementById('repoInput').value.trim();
  const errorMsg = document.getElementById('errorMsg');
  const btn = document.getElementById('analyzeBtn');
  errorMsg.classList.remove('visible');

  if (!input || !input.includes('github.com')) {
    errorMsg.textContent = '⚠ Please enter a valid GitHub URL.';
    errorMsg.classList.add('visible');
    return;
  }

  btn.disabled = true;
  currentRepoUrl = cleanGithubUrl(input);
  document.getElementById('landing').classList.add('hidden');
  setTimeout(() => {
    document.getElementById('loading').classList.add('active');
    animateLoading(input);
  }, 600);
}

async function animateLoading(repoUrl) {
  const steps = ['step1','step2','step3','step4','step5'];
  const bar = document.getElementById('loaderBar');
  let data = null, fetchError = null;

  // Read JSON first so we can surface the `error` field from the API
  const fetchPromise = fetch(`${API_BASE}/api/analyze?repoUrl=${encodeURIComponent(cleanGithubUrl(repoUrl))}`)
    .then(async res => {
      const json = await res.json();
      if (!res.ok || json.error) throw new Error(json.error || 'Could not fetch repository.');
      return json;
    })
    .then(json => { data = json; })
    .catch(err => { fetchError = err; });

  for (let i = 0; i < steps.length; i++) {
    document.getElementById(steps[i]).classList.add('active');
    bar.style.width = ((i + 1) / steps.length * 85) + '%';
    await sleep(600);
    document.getElementById(steps[i]).classList.add('done');
  }

  await fetchPromise;
  bar.style.width = '100%';
  await sleep(300);
  document.getElementById('loading').classList.remove('active');

  if (fetchError || !data) { showError(fetchError?.message); return; }
  currentData = data;
  renderReport(data, repoUrl);
}

// ── Render ────────────────────────────────────────────────────────────────────

function renderReport(data, repoUrl) {
  const parts = repoUrl.replace('https://github.com/', '').split('/');
  document.getElementById('headerRepoLabel').innerHTML = `github.com / <span>${parts[0]}/${parts[1]}</span>`;
  document.getElementById('githubLink').href = `https://github.com/${parts[0]}/${parts[1]}`;
  window.history.pushState({}, '', `/r/${parts[0]}/${parts[1]}`);

  const gradeColor = GRADE_COLORS[data.grade] || '#00ff88';
  const gradeBadge = document.getElementById('gradeBadge');
  gradeBadge.style.background = `rgba(${hexToRgb(gradeColor)},0.1)`;
  gradeBadge.style.border = `1px solid rgba(${hexToRgb(gradeColor)},0.3)`;
  gradeBadge.style.color = gradeColor;
  document.getElementById('gradeDot').style.background = gradeColor;
  document.getElementById('gradeText').textContent = `GRADE ${data.grade} — ${GRADE_LABELS[data.grade]}`;

  document.getElementById('repoName').textContent = data.fullName;
  document.getElementById('repoDesc').textContent = data.description || 'No description provided.';
  document.getElementById('statStars').textContent = formatNum(data.stars);
  document.getElementById('statForks').textContent = formatNum(data.forks);
  document.getElementById('statIssues').textContent = formatNum(data.openIssues);
  document.getElementById('statPushed').textContent = timeAgo(data.lastPushed);

  switchTab('audit');
  const report = document.getElementById('report');
  report.style.display = 'block';
  setTimeout(() => {
    report.classList.add('visible');
    animateScore(data.totalScore, gradeColor);
    renderCategories(data.categories);
    renderSuggestions(data.suggestions);
    renderAnalytics(data);
  }, 50);
}

function animateScore(score, color) {
  const ring = document.getElementById('fillRing');
  const num  = document.getElementById('scoreNum');
  ring.style.stroke = color;
  ring.style.filter = `drop-shadow(0 0 8px ${color})`;
  num.style.color   = color;
  num.style.filter  = `drop-shadow(0 0 20px ${color}40)`;
  ring.style.strokeDashoffset = CIRCUMFERENCE - (score / 100) * CIRCUMFERENCE;
  let c = 0;
  const t = setInterval(() => { num.textContent = ++c; if (c >= score) clearInterval(t); }, 18);
}

function renderCategories(categories) {
  const container = document.getElementById('categoriesContainer');
  container.innerHTML = '';
  categories.forEach((cat, i) => {
    const pct = Math.round((cat.score / cat.maxScore) * 100);
    const barColor = pct >= 80 ? 'var(--good)' : pct >= 50 ? 'var(--mid)' : 'var(--bad)';
    const icon = CATEGORY_ICONS[cat.name] || '📋';
    const checksHtml = cat.checks.map(ck => `
      <div class="check-item">
        <div class="check-icon ${ck.status}">${ck.status==='pass'?'✓':ck.status==='fail'?'✗':'!'}</div>
        <div class="check-text">${ck.label}${ck.detail?`<div class="check-sub">${ck.detail}</div>`:''}</div>
      </div>`).join('');

    const card = document.createElement('div');
    card.className = 'category-card';
    card.innerHTML = `
      <div class="cat-header">
        <div class="cat-title"><span>${icon}</span> ${cat.name}</div>
        <div class="cat-score"><span>${cat.score}</span> / ${cat.maxScore}</div>
      </div>
      <div class="cat-bar-bg"><div class="cat-bar-fill" style="background:${barColor}" data-width="${pct}"></div></div>
      <div class="checks">${checksHtml}</div>`;
    container.appendChild(card);
    setTimeout(() => {
      card.classList.add('animate');
      const bar = card.querySelector('.cat-bar-fill');
      if (bar) bar.style.width = bar.dataset.width + '%';
    }, i * 100);
  });
}

function renderSuggestions(suggestions) {
  const container = document.getElementById('suggestionsContainer');
  container.innerHTML = '';
  if (!suggestions?.length) {
    container.innerHTML = `<div class="no-suggestions">✓ No major issues found. This repo is in great shape!</div>`;
    return;
  }
  suggestions.forEach((s, i) => {
    const link = getSuggestionLink(s);
    const div = document.createElement('div');
    div.className = 'suggestion';
    div.innerHTML = `<span class="priority">${i === 0 ? 'HIGH' : 'LOW'}</span><span class="s-text">${s}</span>${link ? `<a class="learn-link" href="${link}" target="_blank">Learn →</a>` : ''}`;
    container.appendChild(div);
  });
}

function renderAnalytics(data) {
  // Languages
  const langEl = document.getElementById('languagesContainer');
  langEl.innerHTML = '';
  if (data.languages && Object.keys(data.languages).length) {
    const total = Object.values(data.languages).reduce((a, b) => a + b, 0);
    Object.entries(data.languages).forEach(([lang, bytes], i) => {
      const pct = ((bytes / total) * 100).toFixed(1);
      langEl.innerHTML += `
        <div class="lang-item">
          <div class="lang-header"><span>${lang}</span><span class="lang-pct">${pct}%</span></div>
          <div class="lang-bar-bg"><div class="lang-bar-fill" style="width:0%;background:${LANG_COLORS[i % LANG_COLORS.length]}" data-width="${pct}"></div></div>
        </div>`;
    });
    setTimeout(() => langEl.querySelectorAll('.lang-bar-fill').forEach(b => b.style.width = b.dataset.width + '%'), 200);
  } else {
    langEl.innerHTML = '<p style="color:var(--muted);font-size:13px">No language data available.</p>';
  }

  // Contributors
  const contribEl = document.getElementById('contributorsContainer');
  contribEl.innerHTML = '';
  if (data.contributors?.length) {
    data.contributors.forEach((c, i) => {
      const [login, commits, avatar] = c.split('|');
      contribEl.innerHTML += `
        <div class="contributor">
          <img class="contrib-avatar" src="${avatar}" alt="${login}" onerror="this.src='https://github.com/identicons/${login}.png'"/>
          <div><div class="contrib-name">${login}</div><div class="contrib-commits">${formatNum(parseInt(commits))} contributions</div></div>
          ${i === 0 ? '<span class="contrib-badge">TOP</span>' : ''}
        </div>`;
    });
  } else {
    contribEl.innerHTML = '<p style="color:var(--muted);font-size:13px">No contributor data available.</p>';
  }

  // Folder structure
  const folderEl = document.getElementById('folderContainer');
  folderEl.innerHTML = '';
  data.rootFiles?.forEach(file => {
    const isDir = !file.includes('.');
    folderEl.innerHTML += `<div class="folder-item ${isDir ? '' : 'file'}"><span>${isDir ? '📁' : getFileIcon(file)}</span><span>${file}</span></div>`;
  });

  // README
  const readmeEl = document.getElementById('readmeContainer');
  if (data.readmeContent) {
    readmeEl.innerHTML = marked.parse(data.readmeContent);
    readmeEl.style.whiteSpace = 'normal';
  } else {
    readmeEl.textContent = 'No README content available.';
  }
}

// ── PDF ───────────────────────────────────────────────────────────────────────

async function downloadPDF() {
  document.getElementById('pdfOverlay').classList.add('active');
  await sleep(300);
  try {
    const { jsPDF } = window.jspdf;
    const pdf = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const pageW = 210, pageH = 297, m = 15;
    let y = m;
    const dark = [10,10,15], accent = [0,255,136], txt = [232,232,240], muted = [102,102,128], surf = [17,17,24];

    pdf.setFillColor(...dark);  pdf.rect(0, 0, pageW, pageH, 'F');
    pdf.setFillColor(...surf);  pdf.rect(0, 0, pageW, 22, 'F');
    pdf.setTextColor(...accent); pdf.setFontSize(10); pdf.setFont('helvetica', 'bold');
    pdf.text('AUDITA — GITHUB REPOSITORY AUDIT REPORT', m, 14);

    // Badge top-right
    const bc = currentData.grade==='A'?[0,255,136]:currentData.grade==='B'?[0,204,136]:currentData.grade==='C'?[255,204,0]:[255,68,102];
    pdf.setFillColor(...bc); pdf.roundedRect(pageW-m-48, 4, 48, 14, 3, 3, 'F');
    pdf.setTextColor(0,0,0); pdf.setFontSize(7);
    pdf.text('AUDITA SCORE', pageW-m-45, 10);
    pdf.setFontSize(10); pdf.text(`${currentData.totalScore}/100  GRADE ${currentData.grade}`, pageW-m-45, 15);

    y = 32;
    pdf.setTextColor(...txt); pdf.setFontSize(20); pdf.text(currentData.fullName, m, y); y += 8;
    pdf.setTextColor(...muted); pdf.setFontSize(9); pdf.text((currentData.description||'No description').substring(0,80), m, y); y += 12;

    // Score + stats row
    pdf.setFillColor(...surf); pdf.roundedRect(m, y, 50, 28, 3, 3, 'F');
    pdf.setTextColor(...accent); pdf.setFontSize(28); pdf.text(String(currentData.totalScore), m+8, y+18);
    pdf.setFontSize(9); pdf.setTextColor(...muted); pdf.text('/ 100', m+26, y+18);
    pdf.setFontSize(10); pdf.setTextColor(...accent); pdf.text(`GRADE ${currentData.grade}`, m+5, y+25);

    let sx = 75;
    [['Stars',formatNum(currentData.stars)],['Forks',formatNum(currentData.forks)],['Issues',formatNum(currentData.openIssues)],['Pushed',timeAgo(currentData.lastPushed)]].forEach(([k,v]) => {
      pdf.setFillColor(...surf); pdf.roundedRect(sx, y, 30, 28, 2, 2, 'F');
      pdf.setTextColor(...txt); pdf.setFontSize(13); pdf.setFont('helvetica','bold'); pdf.text(v, sx+3, y+14);
      pdf.setTextColor(...muted); pdf.setFontSize(7); pdf.text(k.toUpperCase(), sx+3, y+22);
      sx += 33;
    });
    y += 38;

    pdf.setTextColor(...muted); pdf.setFontSize(8); pdf.setFont('helvetica','normal');
    pdf.text('// CATEGORY BREAKDOWN', m, y); y += 6;
    currentData.categories.forEach(cat => {
      if (y > 260) { pdf.addPage(); pdf.setFillColor(...dark); pdf.rect(0,0,pageW,pageH,'F'); y = m; }
      const pct = Math.round((cat.score/cat.maxScore)*100);
      pdf.setFillColor(...surf); pdf.roundedRect(m, y, pageW-m*2, 18, 2, 2, 'F');
      pdf.setTextColor(...txt); pdf.setFontSize(9); pdf.setFont('helvetica','bold'); pdf.text(cat.name.toUpperCase(), m+4, y+7);
      pdf.setTextColor(...muted); pdf.setFontSize(8); pdf.text(`${cat.score} / ${cat.maxScore}`, pageW-m-20, y+7);
      const bw = pageW-m*2-30;
      pdf.setFillColor(30,30,45); pdf.rect(m+4, y+11, bw, 3, 'F');
      pdf.setFillColor(...(pct>=80?[0,255,136]:pct>=50?[255,204,0]:[255,68,102])); pdf.rect(m+4, y+11, bw*pct/100, 3, 'F');
      y += 22;
    });

    if (currentData.suggestions?.length) {
      y += 4;
      if (y > 240) { pdf.addPage(); pdf.setFillColor(...dark); pdf.rect(0,0,pageW,pageH,'F'); y = m; }
      pdf.setTextColor(...muted); pdf.setFontSize(8); pdf.text('// IMPROVEMENT SUGGESTIONS', m, y); y += 6;
      currentData.suggestions.forEach((s, i) => {
        if (y > 275) { pdf.addPage(); pdf.setFillColor(...dark); pdf.rect(0,0,pageW,pageH,'F'); y = m; }
        pdf.setFillColor(...surf); pdf.roundedRect(m, y, pageW-m*2, 12, 2, 2, 'F');
        pdf.setFillColor(255,107,53); pdf.rect(m, y, 2, 12, 'F');
        pdf.setTextColor(255,107,53); pdf.setFontSize(7); pdf.text(i===0?'HIGH':'LOW', m+5, y+7);
        pdf.setTextColor(...txt); pdf.setFontSize(8); pdf.text((s.length>90?s.substring(0,90)+'...':s), m+18, y+7);
        y += 15;
      });
    }

    pdf.setFillColor(...surf); pdf.rect(0, pageH-12, pageW, 12, 'F');
    pdf.setTextColor(...muted); pdf.setFontSize(7);
    pdf.text(`Generated by Audita • ${new Date().toLocaleDateString()} • ${currentRepoUrl}`, m, pageH-4);
    pdf.save(`audita-report-${currentData.name}.pdf`);
  } catch (e) {
    console.error('PDF error:', e);
    alert('PDF generation failed. Please try again.');
  }
  document.getElementById('pdfOverlay').classList.remove('active');
}

// ── Share / Reset / Error ─────────────────────────────────────────────────────

function copyShareLink() {
  const parts = currentRepoUrl.replace('https://github.com/', '').split('/');
  const url = `${window.location.origin}/r/${parts[0]}/${parts[1]}`;
  navigator.clipboard.writeText(url).then(() => {
    const btn = document.getElementById('shareLinkBtn');
    const orig = btn.textContent;
    btn.textContent = '✓ Copied!';
    btn.style.color = btn.style.borderColor = 'var(--accent)';
    setTimeout(() => { btn.textContent = orig; btn.style.color = btn.style.borderColor = ''; }, 2000);
  });
}

function resetApp() {
  const report = document.getElementById('report');
  report.classList.remove('visible');
  window.history.pushState({}, '', '/');
  setTimeout(() => {
    report.style.display = 'none';
    document.querySelectorAll('.loader-step').forEach(s => s.className = 'loader-step');
    document.getElementById('loaderBar').style.width = '0';
    document.getElementById('categoriesContainer').innerHTML = '';
    document.getElementById('suggestionsContainer').innerHTML = '';
    document.getElementById('scoreNum').textContent = '0';
    document.getElementById('fillRing').style.strokeDashoffset = CIRCUMFERENCE;
    document.getElementById('repoInput').value = '';
    document.getElementById('errorMsg').classList.remove('visible');
    document.getElementById('analyzeBtn').disabled = false;
    currentData = null; currentRepoUrl = null;
    document.getElementById('landing').classList.remove('hidden');
  }, 400);
}

function showError(msg) {
  document.getElementById('landing').classList.remove('hidden');
  document.getElementById('errorMsg').textContent = '⚠ ' + (msg || 'Could not fetch repository. Check the URL or try again.');
  document.getElementById('errorMsg').classList.add('visible');
  document.getElementById('analyzeBtn').disabled = false;
}

// ── Init ──────────────────────────────────────────────────────────────────────

document.getElementById('repoInput').addEventListener('keydown', e => { if (e.key === 'Enter') startAnalysis(); });

// Auto-load shared /r/owner/repo URLs
(function () {
  const path = window.location.pathname;
  if (path.startsWith('/r/')) {
    const repo = path.replace('/r/', '');
    if (repo.includes('/')) {
      document.getElementById('repoInput').value = 'https://github.com/' + repo;
      document.querySelector('.landing p').textContent = 'Loading shared report for ' + repo + '...';
      setTimeout(startAnalysis, 800);
    }
  }
})();

// Landing logo typewriter
(function () {
  const word = 'AUDITA';
  const textEl = document.getElementById('llText');
  const cursor = document.querySelector('.ll-cursor');
  const glitch = '!<>-_\\/[]{}=+*^?#@%&';

  function erase(pos) {
    if (pos < 0) { cursor.classList.remove('visible'); setTimeout(() => typeNext(0), 300); return; }
    textEl.textContent = word.substring(0, pos);
    setTimeout(() => erase(pos - 1), 45 + Math.random() * 30);
  }

  function typeNext(i) {
    if (i >= word.length) { cursor.classList.add('visible'); setTimeout(() => erase(word.length - 1), 1800); return; }
    textEl.textContent = word.substring(0, i) + glitch[Math.floor(Math.random() * glitch.length)];
    setTimeout(() => { textEl.textContent = word.substring(0, i + 1); setTimeout(() => typeNext(i + 1), 80 + Math.random() * 60); }, 55);
  }

  setTimeout(() => typeNext(0), 350);
})();