/* ── compare.js — depends on utils.js ── */

const CIRCUMFERENCE_SM = 2 * Math.PI * 51;
const GRADE_COLORS = { A:'#00ff88', B:'#00cc88', C:'#ffcc00', D:'#ff4466' };
const GRADE_LABELS = { A:'Excellent', B:'Good', C:'Needs Work', D:'Poor' };

// Init rings so dashoffset animation has a length to work against
document.querySelectorAll('.fill-ring').forEach(r => {
  r.style.strokeDasharray  = CIRCUMFERENCE_SM;
  r.style.strokeDashoffset = CIRCUMFERENCE_SM;
});

// ── Comparison flow ───────────────────────────────────────────────────────────

async function startComparison() {
  const url1 = document.getElementById('repo1Input').value.trim();
  const url2 = document.getElementById('repo2Input').value.trim();
  const errorMsg = document.getElementById('errorMsg');
  const btn = document.getElementById('compareBtn');

  errorMsg.classList.remove('visible');

  if (!url1.includes('github.com') || !url2.includes('github.com')) {
    errorMsg.textContent = '⚠ Please enter two valid GitHub URLs.';
    errorMsg.classList.add('visible');
    return;
  }

  btn.disabled = true;
  const inputEl = document.getElementById('compareInput');
  inputEl.style.opacity = '0.3';
  inputEl.style.pointerEvents = 'none';
  document.getElementById('loading').classList.add('active');
  document.getElementById('loaderBar').style.width = '30%';

  try {
    // Fetch both in parallel — surface API error messages per repo
    const [data1, data2] = await Promise.all([
      apiFetch(`${API_BASE}/api/analyze?repoUrl=${encodeURIComponent(cleanGithubUrl(url1))}`).then(async r => {
        const j = await r.json();
        if (!r.ok || j.error) throw new Error('Repo 1: ' + (j.error || 'Could not be fetched.'));
        return j;
      }),
      apiFetch(`${API_BASE}/api/analyze?repoUrl=${encodeURIComponent(cleanGithubUrl(url2))}`).then(async r => {
        const j = await r.json();
        if (!r.ok || j.error) throw new Error('Repo 2: ' + (j.error || 'Could not be fetched.'));
        return j;
      })
    ]);

    document.getElementById('loaderBar').style.width = '100%';
    await sleep(400);
    document.getElementById('loading').classList.remove('active');
    inputEl.style.display = 'none';
    renderComparison(data1, data2);

  } catch (e) {
    document.getElementById('loading').classList.remove('active');
    inputEl.style.opacity = '1';
    inputEl.style.pointerEvents = 'auto';
    errorMsg.textContent = '⚠ ' + (e.message || 'Failed to fetch one or both repositories.');
    errorMsg.classList.add('visible');
    btn.disabled = false;
  }
}

// ── Render ────────────────────────────────────────────────────────────────────

function renderComparison(d1, d2) {
  document.getElementById('results').classList.add('visible');

  const winner = d1.totalScore > d2.totalScore ? d1 : d2.totalScore > d1.totalScore ? d2 : null;
  const diff   = Math.abs(d1.totalScore - d2.totalScore);
  const banner = document.getElementById('winnerBanner');

  if (winner) {
    const c = GRADE_COLORS[winner.grade] || '#00ff88';
    banner.style.background = `rgba(${hexToRgb(c)},0.08)`;
    banner.style.border = `1px solid rgba(${hexToRgb(c)},0.3)`;
    document.getElementById('winnerText').textContent = `${winner.fullName} wins`;
    document.getElementById('winnerText').style.color = c;
    document.getElementById('winnerSub').textContent =
      `Leading by ${diff} point${diff !== 1 ? 's' : ''} — ${winner.totalScore}/100 vs ${(winner===d1?d2:d1).totalScore}/100`;
  } else {
    banner.style.background = 'rgba(255,204,0,0.08)';
    banner.style.border = '1px solid rgba(255,204,0,0.3)';
    document.getElementById('winnerText').textContent = "It's a tie!";
    document.getElementById('winnerText').style.color = 'var(--warn)';
    document.getElementById('winnerSub').textContent = `Both repos scored ${d1.totalScore}/100`;
  }

  renderScoreCard(d1, 1);
  renderScoreCard(d2, 2);

  if (d1.totalScore > d2.totalScore)      document.getElementById('card1').classList.add('winner');
  else if (d2.totalScore > d1.totalScore) document.getElementById('card2').classList.add('winner');

  document.getElementById('tableHeader1').textContent = d1.name;
  document.getElementById('tableHeader2').textContent = d2.name;
  renderCategoryTable(d1, d2);
  renderSuggestions(d1, 1);
  renderSuggestions(d2, 2);
}

function renderScoreCard(data, n) {
  const color = GRADE_COLORS[data.grade] || '#00ff88';
  const ring  = document.getElementById(`ring${n}`);
  const num   = document.getElementById(`scoreNum${n}`);

  ring.style.stroke = color;
  ring.style.filter = `drop-shadow(0 0 6px ${color})`;
  num.style.color   = color;

  const score = data.totalScore ?? 0;
  ring.style.strokeDasharray  = CIRCUMFERENCE_SM;
  ring.style.strokeDashoffset = CIRCUMFERENCE_SM - (score / 100) * CIRCUMFERENCE_SM;

  if (score === 0) {
    num.textContent = '0';
  } else {
    let c = 0;
    const t = setInterval(() => { num.textContent = ++c; if (c >= score) clearInterval(t); }, 20);
  }

  const pill = document.getElementById(`gradePill${n}`);
  pill.style.background = `rgba(${hexToRgb(color)},0.1)`;
  pill.style.border = `1px solid rgba(${hexToRgb(color)},0.3)`;
  pill.style.color  = color;
  document.getElementById(`gradeDot${n}`).style.background = color;

  const grade = data.grade || 'N/A';
  document.getElementById(`gradeText${n}`).textContent =
    grade !== 'N/A' ? `GRADE ${grade} — ${GRADE_LABELS[grade]}` : 'Score unavailable';

  document.getElementById(`repoName${n}`).textContent = data.fullName;
  document.getElementById(`repoDesc${n}`).textContent = (data.description || 'No description').substring(0, 80);
  document.getElementById(`stars${n}`).textContent  = formatNum(data.stars);
  document.getElementById(`forks${n}`).textContent  = formatNum(data.forks);
  document.getElementById(`issues${n}`).textContent = formatNum(data.openIssues);
}

function renderCategoryTable(d1, d2) {
  const container = document.getElementById('categoryTable');
  container.innerHTML = '';
  const d2Map = Object.fromEntries(d2.categories.map(c => [c.name, c]));

  d1.categories.forEach(cat => {
    const cat2 = d2Map[cat.name];
    if (!cat2) return;
    const p1 = Math.round((cat.score / cat.maxScore) * 100);
    const p2 = Math.round((cat2.score / cat2.maxScore) * 100);
    const [c1, c2] = p1 > p2 ? ['win','lose'] : p2 > p1 ? ['lose','win'] : ['tie','tie'];
    const row = document.createElement('div');
    row.className = 'table-row';
    row.innerHTML = `
      <div class="cat-name">${CATEGORY_ICONS[cat.name]||'📋'} ${cat.name}</div>
      <div class="score-cell ${c1}">${cat.score}/${cat.maxScore}</div>
      <div class="score-cell ${c2}">${cat2.score}/${cat2.maxScore}</div>`;
    container.appendChild(row);
  });
}

function renderSuggestions(data, n) {
  document.getElementById(`sugTitle${n}`).textContent = data.fullName;
  const container = document.getElementById(`sugList${n}`);
  container.innerHTML = '';
  if (!data.suggestions?.length) {
    container.innerHTML = `<div class="no-suggestions-sm">✓ No major issues found!</div>`;
    return;
  }
  data.suggestions.forEach(s => {
    const div = document.createElement('div');
    div.className = 'suggestion-item';
    div.textContent = s;
    container.appendChild(div);
  });
}

// ── Reset ─────────────────────────────────────────────────────────────────────

function resetComparison() {
  document.getElementById('results').classList.remove('visible');
  const inputEl = document.getElementById('compareInput');
  inputEl.style.cssText = 'display:block;opacity:1;pointer-events:auto';
  document.getElementById('compareBtn').disabled = false;
  document.getElementById('repo1Input').value = '';
  document.getElementById('repo2Input').value = '';
  document.getElementById('errorMsg').classList.remove('visible');
  document.getElementById('loaderBar').style.width = '0';
  document.getElementById('card1').classList.remove('winner');
  document.getElementById('card2').classList.remove('winner');
  ['ring1','ring2'].forEach(id => {
    document.getElementById(id).style.strokeDasharray  = CIRCUMFERENCE_SM;
    document.getElementById(id).style.strokeDashoffset = CIRCUMFERENCE_SM;
  });
  document.getElementById('scoreNum1').textContent = '0';
  document.getElementById('scoreNum2').textContent = '0';
}

// ── Init ──────────────────────────────────────────────────────────────────────

document.getElementById('repo2Input').addEventListener('keydown', e => { if (e.key === 'Enter') startComparison(); });