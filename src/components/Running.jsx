import React, { useState, useEffect, useRef, useMemo } from 'react';
import '../css/Music.css';
import '../css/Running.css';
import HamburgerMenu from './HamburgerMenu';
import LoadingSpinner from './LoadingSpinner';

const API_URL  = '/api/running.php';
const ACCENT   = '#47A025';

const COOKIE    = 'musicEditToken';
const getCookie = () => { const m = document.cookie.match(new RegExp('(?:^|; )' + COOKIE + '=([^;]*)')); return m ? decodeURIComponent(m[1]) : null; };
const setCookie = (v) => { document.cookie = `${COOKIE}=${encodeURIComponent(v)}; max-age=${14 * 86400}; path=/; SameSite=Strict`; };
const delCookie = ()  => { document.cookie = `${COOKIE}=; max-age=0; path=/; SameSite=Strict`; };

const CURRENT_YEAR = String(new Date().getFullYear());

const DAY_COLORS = {
  mon: '#4C78A8', tue: '#54A24B', wed: '#B279A2',
  thu: '#F58518', fri: '#E45756', sat: '#72B7B2', sun: '#EECA3B',
};
const DAY_LABELS = { mon: 'M', tue: 'T', wed: 'W', thu: 'T', fri: 'F', sat: 'S', sun: 'S' };
const DAYS = ['mon','tue','wed','thu','fri','sat','sun'];
const DAY_FULL = { mon: 'Mon', tue: 'Tue', wed: 'Wed', thu: 'Thu', fri: 'Fri', sat: 'Sat', sun: 'Sun' };

function todayStr() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

function fmtWeekLabel(weekStart) {
  const d = new Date(weekStart + 'T00:00:00');
  const month = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][d.getMonth()];
  return `${month} ${d.getDate()}`;
}

function fmtWeekFull(weekStart) {
  const d = new Date(weekStart + 'T00:00:00');
  const month = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'][d.getMonth()];
  return `${month} ${d.getDate()}, ${d.getFullYear()}`;
}

function parsePaceSeconds(str) {
  if (!str || !str.trim()) return null;
  const parts = str.trim().split(':');
  if (parts.length !== 2) return null;
  const m = parseInt(parts[0], 10);
  const s = parseInt(parts[1], 10);
  if (isNaN(m) || isNaN(s) || m < 0 || s < 0 || s >= 60) return null;
  const total = m * 60 + s;
  return total > 0 ? total : null;
}

function formatPaceSeconds(seconds) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

function calculateStreaks(sortedWeeks) {
  if (sortedWeeks.length === 0) return { longest: 0, current: 0 };
  let maxStreak = 1;
  let streak = 1;
  for (let i = 1; i < sortedWeeks.length; i++) {
    const prev = new Date(sortedWeeks[i - 1].weekStart + 'T00:00:00');
    const curr = new Date(sortedWeeks[i].weekStart + 'T00:00:00');
    const daysDiff = Math.round((curr - prev) / 86400000);
    if (daysDiff === 7) {
      streak++;
      maxStreak = Math.max(maxStreak, streak);
    } else {
      streak = 1;
    }
  }
  const lastWeek = new Date(sortedWeeks[sortedWeeks.length - 1].weekStart + 'T00:00:00');
  const daysSinceLast = Math.round((new Date() - lastWeek) / 86400000);
  return { longest: maxStreak, current: daysSinceLast <= 13 ? streak : 0 };
}

// ─── Running chart ────────────────────────────────────────────────────────────

const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

const RunningChart = ({ weeks, yearFilter }) => {
  const wrapRef   = useRef(null);
  const scrollRef = useRef(null);
  const [tip, setTip]          = useState(null);
  const [containerW, setContW] = useState(0);

  useEffect(() => {
    const el = wrapRef.current;
    if (!el) return;
    const ro = new ResizeObserver(([entry]) => setContW(entry.contentRect.width));
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const chronoWeeks = useMemo(() => [...weeks], [weeks]);
  const maxTotal    = useMemo(() => Math.max(...chronoWeeks.map(w => w.total), 1), [chronoWeeks]);

  const BAR_H      = 96;
  const PAD_L      = 16;
  const PAD_R      = 16;
  const MIN_ITEM_W = 24;
  const isYear     = yearFilter !== 'all';

  let BAR_W, BAR_GAP, ITEM_W;
  if (isYear && containerW > 0 && chronoWeeks.length > 0) {
    const stretch = (containerW - PAD_L - PAD_R) / chronoWeeks.length;
    if (stretch >= MIN_ITEM_W) {
      ITEM_W  = stretch;
      BAR_GAP = Math.max(3, Math.round(ITEM_W * 0.22));
      BAR_W   = ITEM_W - BAR_GAP;
    } else {
      ITEM_W  = MIN_ITEM_W;
      BAR_GAP = 4;
      BAR_W   = MIN_ITEM_W - BAR_GAP;
    }
  } else {
    BAR_W   = 22;
    BAR_GAP = 8;
    ITEM_W  = BAR_W + BAR_GAP;
  }

  const useMonthLabels = ITEM_W < 30;
  const SVG_H          = BAR_H + 38;
  const computedSvgW   = PAD_L + chronoWeeks.length * ITEM_W + PAD_R;
  const svgW           = isYear && containerW > 0 && computedSvgW <= containerW ? containerW : computedSvgW;

  useEffect(() => {
    if (scrollRef.current) scrollRef.current.scrollLeft = scrollRef.current.scrollWidth;
  }, [weeks.length]);

  return (
    <div className="runChartWrap" ref={wrapRef}>
      <div className="runChartLegend">
        {DAYS.map(d => (
          <span key={d} className="runChartLegendItem">
            <span className="runChartLegendDot" style={{ background: DAY_COLORS[d] }} />
            {DAY_FULL[d]}
          </span>
        ))}
      </div>
      <div className="runChartScroll" ref={scrollRef}>
        <svg width={svgW} height={SVG_H} className="runChartSvg">
          {chronoWeeks.map((week, i) => {
            const x = PAD_L + i * ITEM_W;
            let yOff = BAR_H;
            const segments = DAYS.map(day => {
              const h = week[day] > 0 ? Math.max((week[day] / maxTotal) * BAR_H, 2) : 0;
              yOff -= h;
              return { day, h, y: yOff };
            }).filter(s => s.h > 0);

            let label = null;
            if (useMonthLabels) {
              const m  = new Date(week.weekStart + 'T00:00:00').getMonth();
              const pm = i > 0 ? new Date(chronoWeeks[i - 1].weekStart + 'T00:00:00').getMonth() : -1;
              if (m !== pm) label = MONTHS[m];
            } else {
              label = fmtWeekLabel(week.weekStart);
            }

            const showValue = week.total > 0 && BAR_W >= 18;

            return (
              <g key={week.weekStart}
                onMouseEnter={(e) => setTip({ week, x: e.clientX, y: e.clientY })}
                onMouseLeave={() => setTip(null)}
                style={{ cursor: 'default' }}>
                <rect x={x} y={0} width={BAR_W} height={BAR_H} fill="#eeece9" rx="2" />
                {segments.map(({ day, h, y }, si) => {
                  const isTop = si === segments.length - 1;
                  const isBot = si === 0;
                  const R = 2;
                  const tl = isTop ? R : 0, tr = isTop ? R : 0;
                  const bl = isBot ? R : 0, br = isBot ? R : 0;
                  const d = [
                    `M ${x+tl},${y}`, `L ${x+BAR_W-tr},${y}`,
                    tr ? `Q ${x+BAR_W},${y} ${x+BAR_W},${y+tr}` : '',
                    `L ${x+BAR_W},${y+h-br}`,
                    br ? `Q ${x+BAR_W},${y+h} ${x+BAR_W-br},${y+h}` : '',
                    `L ${x+bl},${y+h}`,
                    bl ? `Q ${x},${y+h} ${x},${y+h-bl}` : '',
                    `L ${x},${y+tl}`,
                    tl ? `Q ${x},${y} ${x+tl},${y}` : '',
                    'Z',
                  ].join(' ');
                  return <path key={day} d={d} fill={DAY_COLORS[day]} />;
                })}
                {label && (
                  <text x={x + BAR_W / 2} y={BAR_H + 17} textAnchor="middle"
                    fontSize="9.5" fill="#999" fontWeight={useMonthLabels ? '600' : '400'}
                    fontFamily="Calibri, sans-serif">
                    {label}
                  </text>
                )}
                {showValue && (
                  <text x={x + BAR_W / 2} y={Math.max((1 - week.total / maxTotal) * BAR_H - 4, 10)}
                    textAnchor="middle" fontSize="8.5" fill="#222"
                    fontFamily="Calibri, sans-serif">
                    {week.total.toFixed(2)}
                  </text>
                )}
              </g>
            );
          })}
        </svg>
      </div>

      {tip && (
        <div className="runChartTip" style={{ left: tip.x + 12, top: tip.y - 100 }}>
          <div className="runChartTipWeek">{fmtWeekFull(tip.week.weekStart)}</div>
          <div className="runChartTipTotal"><strong>{tip.week.total.toFixed(2)}</strong> mi total</div>
          <div className="runChartTipDays">
            {DAYS.filter(d => tip.week[d] > 0).map(d => (
              <div key={d} className="runChartTipDay">
                <span className="runChartTipDot" style={{ background: DAY_COLORS[d] }} />
                {DAY_FULL[d]}: {tip.week[d].toFixed(2)} mi
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

// ─── Password modal ───────────────────────────────────────────────────────────

const PasswordModal = ({ onSubmit, onCancel }) => {
  const [pw, setPw] = useState('');
  return (
    <div className="modalOverlay" onClick={onCancel}>
      <div className="modalBox modalBox--narrow" onClick={e => e.stopPropagation()}>
        <h3 className="modalTitle">Edit Mode</h3>
        <form onSubmit={e => { e.preventDefault(); if (pw.trim()) onSubmit(pw.trim()); }}>
          <label className="modalLabel">Password</label>
          <input className="modalInput" type="password" value={pw} onChange={e => setPw(e.target.value)} autoFocus />
          <div className="modalActions">
            <div className="modalActionsRight">
              <button type="button" className="modalCancelBtn" onClick={onCancel}>Cancel</button>
              <button type="submit" className="modalSaveBtn" disabled={!pw.trim()}>Enter</button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

// ─── Add run modal ────────────────────────────────────────────────────────────

const AddRunModal = ({ onSave, onCancel }) => {
  const [miles,   setMiles]   = useState('');
  const [date,    setDate]    = useState(todayStr());
  const [pace,    setPace]    = useState('');
  const [saving,  setSaving]  = useState(false);
  const [result,  setResult]  = useState(null);

  const paceSeconds  = parsePaceSeconds(pace);
  const paceInvalid  = pace.trim() !== '' && paceSeconds === null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!miles || parseFloat(miles) <= 0) return;
    setSaving(true); setResult(null);
    try {
      await onSave({ miles: parseFloat(miles), date, pace: paceSeconds != null ? `${Math.floor(paceSeconds/60)}:${String(paceSeconds%60).padStart(2,'0')}` : undefined });
      setResult({ ok: true });
      setTimeout(onCancel, 1500);
    } catch (err) {
      setSaving(false);
      setResult({ ok: false, message: err.message || 'Something went wrong.' });
    }
  };

  return (
    <div className="modalOverlay" onClick={saving ? undefined : onCancel}>
      <form className="modalBox modalBox--narrow" style={{ position: 'relative' }}
        onClick={e => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3 className="modalTitle">Log Run</h3>
        <label className="modalLabel">Miles</label>
        <input className="modalInput" type="number" step="0.01" min="0" value={miles}
          onChange={e => setMiles(e.target.value)} autoFocus placeholder="e.g. 6.2" />
        <label className="modalLabel">Date</label>
        <input className="modalInput" type="date" value={date}
          onChange={e => setDate(e.target.value)} />
        <label className="modalLabel">Avg Pace <span className="modalLabelOptional">(optional, MM:SS/mi)</span></label>
        <input className={`modalInput${paceInvalid ? ' modalInput--error' : ''}`} type="text"
          value={pace} onChange={e => setPace(e.target.value)} placeholder="e.g. 8:30" />
        {paceInvalid && <p className="modalInputError">Enter pace as M:SS (e.g. 8:30)</p>}
        <div className="modalActions">
          <div className="modalActionsRight">
            <button type="button" className="modalCancelBtn" onClick={onCancel} disabled={saving}>Cancel</button>
            <button type="submit" className="modalSaveBtn" disabled={saving || !miles || parseFloat(miles) <= 0}>
              {saving ? <><span className="btnSpinner" />Saving…</> : 'Save'}
            </button>
          </div>
        </div>
        {result && (
          <div className={`modalResultOverlay${result.ok ? ' modalResultOverlay--ok' : ' modalResultOverlay--err'}`}>
            <div className="modalResultIcon">{result.ok ? '✓' : '✗'}</div>
            <p className="modalResultMsg">{result.ok ? 'Logged!' : result.message}</p>
            {!result.ok && (
              <div className="modalResultBtns">
                <button type="button" className="modalCancelBtn" onClick={() => setResult(null)}>Try Again</button>
                <button type="button" className="modalCancelBtn" onClick={onCancel}>Close</button>
              </div>
            )}
          </div>
        )}
      </form>
    </div>
  );
};

// ─── Edit run modal ───────────────────────────────────────────────────────────

const EditRunModal = ({ entry, onSave, onCancel }) => {
  const [miles,  setMiles]  = useState(String(parseFloat(entry.miles)));
  const [date,   setDate]   = useState(entry.date);
  const [pace,   setPace]   = useState(entry.paceSeconds != null ? formatPaceSeconds(entry.paceSeconds) : '');
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState(null);

  const paceSeconds = parsePaceSeconds(pace);
  const paceInvalid = pace.trim() !== '' && paceSeconds === null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!miles || parseFloat(miles) <= 0) return;
    setSaving(true); setResult(null);
    try {
      await onSave({
        id: entry.id, miles: parseFloat(miles), date,
        pace: paceSeconds != null ? `${Math.floor(paceSeconds/60)}:${String(paceSeconds%60).padStart(2,'0')}` : '',
      });
      setResult({ ok: true });
      setTimeout(onCancel, 1500);
    } catch (err) {
      setSaving(false);
      setResult({ ok: false, message: err.message || 'Something went wrong.' });
    }
  };

  return (
    <div className="modalOverlay" onClick={saving ? undefined : onCancel}>
      <form className="modalBox modalBox--narrow" style={{ position: 'relative' }}
        onClick={e => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3 className="modalTitle">Edit Run</h3>
        <label className="modalLabel">Miles</label>
        <input className="modalInput" type="number" step="0.01" min="0" value={miles}
          onChange={e => setMiles(e.target.value)} autoFocus placeholder="e.g. 6.2" />
        <label className="modalLabel">Date</label>
        <input className="modalInput" type="date" value={date}
          onChange={e => setDate(e.target.value)} />
        <label className="modalLabel">Avg Pace <span className="modalLabelOptional">(optional, MM:SS/mi)</span></label>
        <input className={`modalInput${paceInvalid ? ' modalInput--error' : ''}`} type="text"
          value={pace} onChange={e => setPace(e.target.value)} placeholder="e.g. 8:30" />
        {paceInvalid && <p className="modalInputError">Enter pace as M:SS (e.g. 8:30)</p>}
        <div className="modalActions">
          <div className="modalActionsRight">
            <button type="button" className="modalCancelBtn" onClick={onCancel} disabled={saving}>Cancel</button>
            <button type="submit" className="modalSaveBtn"
              disabled={saving || !miles || parseFloat(miles) <= 0 || paceInvalid}>
              {saving ? <><span className="btnSpinner" />Saving…</> : 'Save'}
            </button>
          </div>
        </div>
        {result && (
          <div className={`modalResultOverlay${result.ok ? ' modalResultOverlay--ok' : ' modalResultOverlay--err'}`}>
            <div className="modalResultIcon">{result.ok ? '✓' : '✗'}</div>
            <p className="modalResultMsg">{result.ok ? 'Updated!' : result.message}</p>
            {!result.ok && (
              <div className="modalResultBtns">
                <button type="button" className="modalCancelBtn" onClick={() => setResult(null)}>Try Again</button>
                <button type="button" className="modalCancelBtn" onClick={onCancel}>Close</button>
              </div>
            )}
          </div>
        )}
      </form>
    </div>
  );
};

// ─── Stats view ───────────────────────────────────────────────────────────────

const StatCard = ({ label, value, subtitle, accent }) => (
  <div className={`runStatCard${accent ? ' runStatCard--accent' : ''}`}>
    <div className="runStatValue">{value}</div>
    {subtitle && <div className="runStatSubtitle">{subtitle}</div>}
    <div className="runStatLabel">{label}</div>
  </div>
);

const StatSection = ({ title }) => (
  <h3 className="runStatsSectionTitle">{title}</h3>
);

const DayFrequencyChart = ({ allEntries }) => {
  const dayCounts = useMemo(() => {
    const counts = [0, 0, 0, 0, 0, 0, 0]; // Mon=0 … Sun=6
    allEntries.forEach(entry => {
      if (!entry.date) return;
      const d = new Date(entry.date + 'T00:00:00');
      const dow = (d.getDay() + 6) % 7;
      counts[dow]++;
    });
    return counts;
  }, [allEntries]);

  const maxCount = Math.max(...dayCounts, 1);
  const dayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  return (
    <div className="runDayChart">
      {dayCounts.map((count, i) => (
        <div key={dayLabels[i]} className="runDayBar">
          <div className="runDayBarCount">{count > 0 ? count : ''}</div>
          <div className="runDayBarTrack">
            {count > 0 && (
              <div
                className="runDayBarFill"
                style={{ height: `${(count / maxCount) * 100}%`, background: DAY_COLORS[DAYS[i]] }}
              />
            )}
          </div>
          <div className="runDayBarLabel" style={{ color: count > 0 ? DAY_COLORS[DAYS[i]] : '#ccc' }}>
            {dayLabels[i]}
          </div>
        </div>
      ))}
    </div>
  );
};

const RunningStats = ({ allWeeks }) => {
  const today      = new Date();
  const thisYear   = today.getFullYear();
  const thisMonth  = today.getMonth() + 1;

  const allEntries  = useMemo(() => allWeeks.flatMap(w => w.entries || []), [allWeeks]);
  const totalMiles  = useMemo(() => allWeeks.reduce((s, w) => s + w.total, 0), [allWeeks]);
  const totalRuns   = allEntries.length;
  const avgWeekly   = allWeeks.length > 0 ? totalMiles / allWeeks.length : 0;

  const bestWeek    = useMemo(() =>
    allWeeks.reduce((best, w) => (!best || w.total > best.total) ? w : best, null),
  [allWeeks]);

  const yearMiles   = useMemo(() =>
    allWeeks.filter(w => w.year === thisYear).reduce((s, w) => s + w.total, 0),
  [allWeeks, thisYear]);

  const monthMiles  = useMemo(() =>
    allEntries.filter(e => {
      const [y, m] = e.date.split('-').map(Number);
      return y === thisYear && m === thisMonth;
    }).reduce((s, e) => s + parseFloat(e.miles), 0),
  [allEntries, thisYear, thisMonth]);

  const { longest: longestStreak, current: currentStreak } = useMemo(() => {
    const sorted = [...allWeeks].sort((a, b) => a.weekStart.localeCompare(b.weekStart));
    return calculateStreaks(sorted);
  }, [allWeeks]);

  const favDay = useMemo(() => {
    const totals = DAYS.map(d => ({ day: DAY_FULL[d], total: allWeeks.reduce((s, w) => s + (w[d] || 0), 0) }));
    const best = totals.reduce((b, d) => d.total > (b?.total || 0) ? d : b, null);
    return best && best.total > 0 ? best.day : null;
  }, [allWeeks]);

  const avgRunDist    = totalRuns > 0 ? totalMiles / totalRuns : 0;
  const longestRun    = useMemo(() => allEntries.length > 0 ? Math.max(...allEntries.map(e => parseFloat(e.miles))) : null, [allEntries]);
  const daysSinceLast = useMemo(() => {
    if (allEntries.length === 0) return null;
    const lastDate = allEntries.reduce((a, b) => a.date > b.date ? a : b).date;
    const diff = Math.round((today - new Date(lastDate + 'T00:00:00')) / 86400000);
    return diff;
  }, [allEntries]);
  const bestMonth     = useMemo(() => {
    const byMonth = {};
    allEntries.forEach(e => {
      const ym = e.date.slice(0, 7);
      byMonth[ym] = (byMonth[ym] || 0) + parseFloat(e.miles);
    });
    const best = Object.entries(byMonth).reduce((a, b) => b[1] > a[1] ? b : a, ['', 0]);
    if (!best[0]) return null;
    const [y, m] = best[0].split('-');
    const monthNames = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
    return { label: `${monthNames[parseInt(m,10)-1]} ${y}`, miles: best[1] };
  }, [allEntries]);

  const pacedEntries = useMemo(() => allEntries.filter(e => e.paceSeconds != null), [allEntries]);
  const avgPace      = pacedEntries.length > 0
    ? Math.round(pacedEntries.reduce((s, e) => s + e.paceSeconds, 0) / pacedEntries.length)
    : null;
  const bestPace     = pacedEntries.length > 0
    ? Math.min(...pacedEntries.map(e => e.paceSeconds))
    : null;

  return (
    <div className="musicPage runStatsPage">
      <StatSection title="Overview" />
      <div className="runStatsGrid">
        <StatCard label="Total Miles" value={totalMiles.toFixed(2)} accent />
        <StatCard label="Total Runs" value={totalRuns} />
        <StatCard label="Weeks Logged" value={allWeeks.length} />
        <StatCard label="Avg / Week" value={`${avgWeekly.toFixed(2)} mi`} />
        <StatCard label="Avg Run" value={totalRuns > 0 ? `${avgRunDist.toFixed(2)} mi` : '—'} />
        <StatCard label="Longest Run" value={longestRun != null ? `${longestRun.toFixed(2)} mi` : '—'} />
      </div>

      <StatSection title="Recent" />
      <div className="runStatsGrid">
        <StatCard label="This Year" value={`${yearMiles.toFixed(2)} mi`} />
        <StatCard label="This Month" value={`${monthMiles.toFixed(2)} mi`} />
        <StatCard label="Current Streak" value={currentStreak > 0 ? `${currentStreak} wks` : '—'} />
        <StatCard label="Longest Streak" value={longestStreak > 0 ? `${longestStreak} wks` : '—'} />
        <StatCard label="Days Since Last Run" value={daysSinceLast != null ? `${daysSinceLast}d` : '—'} />
      </div>

      <StatSection title="Records" />
      <div className="runStatsGrid">
        <StatCard
          label="Best Week"
          value={bestWeek ? `${bestWeek.total.toFixed(2)} mi` : '—'}
          subtitle={bestWeek ? fmtWeekFull(bestWeek.weekStart) : null}
        />
        <StatCard
          label="Best Month"
          value={bestMonth ? `${bestMonth.miles.toFixed(2)} mi` : '—'}
          subtitle={bestMonth ? bestMonth.label : null}
        />
        <StatCard label="Favorite Day" value={favDay || '—'} />
      </div>

      <StatSection title="Day Breakdown" />
      <DayFrequencyChart allEntries={allEntries} />

      {pacedEntries.length > 0 && (
        <>
          <StatSection title="Pace" />
          <div className="runStatsGrid">
            <StatCard label="Avg Pace" value={avgPace != null ? `${formatPaceSeconds(avgPace)}/mi` : '—'} />
            <StatCard label="Best Pace" value={bestPace != null ? `${formatPaceSeconds(bestPace)}/mi` : '—'} />
            <StatCard label="Pace Logged" value={`${pacedEntries.length} of ${totalRuns} runs`} />
          </div>
        </>
      )}
    </div>
  );
};

// ─── Week row ─────────────────────────────────────────────────────────────────

const WeekRow = ({ week, isEditing, onDeleteEntry, onEditEntry }) => {
  const [expanded, setExpanded] = useState(false);
  const [confirmId, setConfirmId] = useState(null);
  const hasEntries = week.entries && week.entries.length > 0;

  return (
    <>
      <tr
        className={`runRow${hasEntries && isEditing ? ' runRow--clickable' : ''}`}
        onClick={() => isEditing && hasEntries && setExpanded(v => !v)}
      >
        <td className="runCell runCell--week">{fmtWeekFull(week.weekStart)}</td>
        {DAYS.map(d => (
          <td key={d} className="runCell runCell--day"
            style={{ color: week[d] > 0 ? DAY_COLORS[d] : undefined }}>
            {week[d] > 0 ? week[d].toFixed(2) : <span className="runDash">—</span>}
          </td>
        ))}
        <td className="runCell runCell--total">{week.total.toFixed(2)}</td>
      </tr>
      {expanded && week.entries.map(entry => (
        <tr key={entry.id} className="runRowEntry">
          <td className="runCell runCell--entryDate" colSpan={8}>
            {entry.date}: {parseFloat(entry.miles).toFixed(2)} mi
            {entry.paceSeconds != null && (
              <span className="runEntryPace"> · {formatPaceSeconds(entry.paceSeconds)}/mi</span>
            )}
          </td>
          <td className="runCell" style={{ whiteSpace: 'nowrap' }}>
            {confirmId === entry.id ? (
              <span className="runDeleteConfirm">
                <button className="runConfirmYes" onClick={() => { setConfirmId(null); onDeleteEntry(entry.id); }}>Yes</button>
                <button className="runConfirmNo" onClick={() => setConfirmId(null)}>No</button>
              </span>
            ) : (
              <>
                <button className="runEditBtn" onClick={e => { e.stopPropagation(); onEditEntry(entry); }} title="Edit">✎</button>
                <button className="runDeleteBtn" onClick={e => { e.stopPropagation(); setConfirmId(entry.id); }}>×</button>
              </>
            )}
          </td>
        </tr>
      ))}
    </>
  );
};

// ─── Main component ───────────────────────────────────────────────────────────

const Running = () => {
  useEffect(() => { document.title = 'Running | My Tracking'; }, []);
  const [weeks,      setWeeks]    = useState([]);
  const [loading,    setLoading]  = useState(true);
  const [fetchError, setFetchErr] = useState(false);
  const [editToken,  setToken]    = useState(null);
  const [showPass,   setShowPass] = useState(false);
  const [showAdd,    setShowAdd]  = useState(false);
  const [showEdit,   setShowEdit] = useState(null);
  const [pendingAdd, setPending]  = useState(false);
  const [saveError,  setSaveErr]  = useState('');
  const [yearFilter, setYearFilter] = useState(CURRENT_YEAR);
  const [activeView, setActiveView] = useState('log'); // 'log' | 'stats'

  useEffect(() => { const t = getCookie(); if (t) setToken(t); }, []);

  const loadWeeks = (year = 'all') => {
    const url = year === 'all' ? API_URL : `${API_URL}?year=${year}`;
    fetch(url)
      .then(r => r.ok ? r.json() : Promise.reject())
      .then(data => { setWeeks(data); setLoading(false); })
      .catch(() => { setFetchErr(true); setLoading(false); });
  };

  useEffect(() => { loadWeeks(CURRENT_YEAR); }, []);

  const [allWeeks, setAllWeeks] = useState([]);
  useEffect(() => {
    fetch(API_URL)
      .then(r => r.ok ? r.json() : [])
      .then(data => setAllWeeks(data))
      .catch(() => {});
  }, []);

  const availableYears = useMemo(() => {
    const yrs = [...new Set(allWeeks.map(w => w.year))].sort((a,b) => b - a);
    return yrs;
  }, [allWeeks]);

  const handleYearChange = (yr) => {
    setYearFilter(yr);
    setLoading(true);
    loadWeeks(yr);
  };

  const enterEdit = (pw) => {
    setToken(pw); setCookie(pw); setShowPass(false);
    if (pendingAdd) { setPending(false); setShowAdd(true); }
  };
  const handleAddNew = () => {
    if (editToken) setShowAdd(true);
    else { setPending(true); setShowPass(true); }
  };
  const exitEdit = () => { setToken(null); delCookie(); };

  const saveRun = async ({ miles, date, pace }) => {
    let res;
    try {
      const body = { miles, date };
      if (pace) body.pace = pace;
      res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify(body),
      });
    } catch { throw new Error('Network error — please try again.'); }
    if (res.status === 401) { setToken(null); delCookie(); throw new Error('Incorrect password.'); }
    if (!res.ok) throw new Error('Save failed — please try again.');
    loadWeeks(yearFilter);
    fetch(API_URL).then(r => r.ok ? r.json() : []).then(setAllWeeks).catch(() => {});
  };

  const updateRun = async ({ id, miles, date, pace }) => {
    let res;
    try {
      res = await fetch(`${API_URL}?id=${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify({ miles, date, pace }),
      });
    } catch { throw new Error('Network error — please try again.'); }
    if (res.status === 401) { setToken(null); delCookie(); throw new Error('Incorrect password.'); }
    if (!res.ok) throw new Error('Update failed — please try again.');
    loadWeeks(yearFilter);
    fetch(API_URL).then(r => r.ok ? r.json() : []).then(setAllWeeks).catch(() => {});
  };

  const deleteEntry = async (id) => {
    try {
      await fetch(`${API_URL}?id=${id}`, { method: 'DELETE', headers: { 'X-Edit-Token': editToken } });
      loadWeeks(yearFilter);
      fetch(API_URL).then(r => r.ok ? r.json() : []).then(setAllWeeks).catch(() => {});
    } catch {}
  };

  const isEditing = !!editToken;

  const yearTotal = useMemo(() => weeks.reduce((s, w) => s + w.total, 0), [weeks]);

  if (loading)    return <LoadingSpinner type="running" />;
  if (fetchError) return <div className="musicOuter musicLoading">Could not load data.</div>;

  const chartWeeks = allWeeks.filter(w => yearFilter === 'all' || w.year === parseInt(yearFilter));
  const chartWeeksDesc = [...chartWeeks].sort((a,b) => a.weekStart.localeCompare(b.weekStart));

  return (
    <div className={`musicOuter runningOuter${isEditing ? ' musicOuter--editing' : ''}`}>
      {isEditing && (
        <div className="editBanner">
          <span className="editBannerLabel">EDIT MODE</span>
          <button className="editBannerDone" onClick={exitEdit}>Done</button>
        </div>
      )}

      {/* Sticky chart area — only on log view */}
      {activeView === 'log' && (
        <div className="runningChartArea">
          <div className="musicHeaderInner" style={{ paddingTop: 28, paddingBottom: 12 }}>
            <div className="musicHeaderRow">
              <HamburgerMenu />
              <p className="musicEyebrow">Running</p>
            </div>
            <h1 className="musicTitle" style={{ fontSize: 36 }}>Mileage Log</h1>
            <div className="musicHeaderRule" style={{ marginBottom: 12 }} />
          </div>
          {chartWeeksDesc.length > 0 && (
            <div className="runChartOuter">
              <RunningChart weeks={chartWeeksDesc} yearFilter={yearFilter} />
            </div>
          )}
        </div>
      )}

      {/* Scrollable content */}
      <div className="runningScrollArea">
        {activeView === 'stats' ? (
          <RunningStats allWeeks={allWeeks} />
        ) : (
          <div className="musicPage" style={{ paddingTop: 32 }}>
            {saveError && <p className="saveError">{saveError}</p>}

            <div className="runYearBar">
              <select className="runYearSelect" value={yearFilter} onChange={e => handleYearChange(e.target.value)}>
                <option value="all">All Years</option>
                {availableYears.map(y => <option key={y} value={y}>{y}</option>)}
              </select>
              {weeks.length > 0 && (
                <span className="runYearTotal">
                  {yearFilter === 'all' ? 'Total' : yearFilter}: <strong>{yearTotal.toFixed(2)} mi</strong>
                  <span className="runYearWeeks"> · {weeks.length} weeks</span>
                </span>
              )}
            </div>

            {weeks.length === 0 ? (
              <p className="allEmpty">No runs logged yet — click Add Run to get started.</p>
            ) : (
              <div className="runTableWrap">
                <table className="runTable">
                  <thead>
                    <tr>
                      <th className="runTh runTh--week">Week of</th>
                      {DAYS.map(d => (
                        <th key={d} className="runTh runTh--day" style={{ color: DAY_COLORS[d] }}>
                          {DAY_LABELS[d]}
                        </th>
                      ))}
                      <th className="runTh runTh--total">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {weeks.map(week => (
                      <WeekRow key={week.weekStart} week={week}
                        isEditing={isEditing} onDeleteEntry={deleteEntry}
                        onEditEntry={entry => setShowEdit(entry)} />
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>

      <nav className="bottomNav">
        <button className={`bottomNavBtn bottomNavBtn--add${isEditing ? ' bottomNavBtn--active' : ''}`}
          onClick={handleAddNew}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          <span>Add Run</span>
        </button>
        <div className="bottomNavDivider" />
        <button
          className={`bottomNavBtn bottomNavBtn--tab${activeView === 'log' ? ' bottomNavBtn--tabActive' : ''}`}
          onClick={() => setActiveView('log')}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/>
            <line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/>
          </svg>
          <span>Log</span>
        </button>
        <button
          className={`bottomNavBtn bottomNavBtn--tab${activeView === 'stats' ? ' bottomNavBtn--tabActive' : ''}`}
          onClick={() => setActiveView('stats')}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
            <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
          </svg>
          <span>Stats</span>
        </button>
      </nav>

      {showPass && (
        <PasswordModal onSubmit={enterEdit} onCancel={() => { setShowPass(false); setPending(false); }} />
      )}
      {showAdd && (
        <AddRunModal onSave={saveRun} onCancel={() => setShowAdd(false)} />
      )}
      {showEdit && (
        <EditRunModal entry={showEdit} onSave={updateRun} onCancel={() => setShowEdit(null)} />
      )}
    </div>
  );
};

export default Running;
