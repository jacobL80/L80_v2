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

  // When bars are narrow, show month names at boundaries instead of every week date
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

            // Month-boundary label vs per-week label
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
                    {week.total.toFixed(1)}
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
          <div className="runChartTipTotal"><strong>{tip.week.total.toFixed(1)}</strong> mi total</div>
          <div className="runChartTipDays">
            {DAYS.filter(d => tip.week[d] > 0).map(d => (
              <div key={d} className="runChartTipDay">
                <span className="runChartTipDot" style={{ background: DAY_COLORS[d] }} />
                {DAY_FULL[d]}: {tip.week[d].toFixed(1)} mi
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
  const [miles, setMiles]   = useState('');
  const [date,  setDate]    = useState(todayStr());
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!miles || parseFloat(miles) <= 0) return;
    setSaving(true); setResult(null);
    try {
      await onSave({ miles: parseFloat(miles), date });
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

// ─── Week row ─────────────────────────────────────────────────────────────────

const WeekRow = ({ week, isEditing, onDeleteEntry }) => {
  const [expanded, setExpanded] = useState(false);
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
          <td className="runCell runCell--entryDate" colSpan={8}>{entry.date}: {parseFloat(entry.miles).toFixed(2)} mi</td>
          <td className="runCell">
            <button className="runDeleteBtn" onClick={() => onDeleteEntry(entry.id)}>×</button>
          </td>
        </tr>
      ))}
    </>
  );
};

// ─── Main component ───────────────────────────────────────────────────────────

const Running = () => {
  const [weeks,      setWeeks]    = useState([]);
  const [loading,    setLoading]  = useState(true);
  const [fetchError, setFetchErr] = useState(false);
  const [editToken,  setToken]    = useState(null);
  const [showPass,   setShowPass] = useState(false);
  const [showAdd,    setShowAdd]  = useState(false);
  const [pendingAdd, setPending]  = useState(false);
  const [saveError,  setSaveErr]  = useState('');
  const [yearFilter, setYearFilter] = useState(CURRENT_YEAR);

  useEffect(() => { const t = getCookie(); if (t) setToken(t); }, []);

  const loadWeeks = (year = 'all') => {
    const url = year === 'all' ? API_URL : `${API_URL}?year=${year}`;
    fetch(url)
      .then(r => r.ok ? r.json() : Promise.reject())
      .then(data => { setWeeks(data); setLoading(false); })
      .catch(() => { setFetchErr(true); setLoading(false); });
  };

  useEffect(() => { loadWeeks(CURRENT_YEAR); }, []);

  // Derive available years from all-time data
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

  const saveRun = async ({ miles, date }) => {
    let res;
    try {
      res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify({ miles, date }),
      });
    } catch { throw new Error('Network error — please try again.'); }
    if (res.status === 401) { setToken(null); delCookie(); throw new Error('Incorrect password.'); }
    if (!res.ok) throw new Error('Save failed — please try again.');
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

  // Compute year total for display
  const yearTotal = useMemo(() => weeks.reduce((s, w) => s + w.total, 0), [weeks]);

  if (loading)    return <LoadingSpinner type="running" />;
  if (fetchError) return <div className="musicOuter musicLoading">Could not load data.</div>;

  // Chart shows all weeks (not year-filtered) for context
  const chartWeeks = allWeeks.filter(w => yearFilter === 'all' || w.year === parseInt(yearFilter));
  // But display newest-first in chart by reversing (chart component reverses again)
  const chartWeeksDesc = [...chartWeeks].sort((a,b) => a.weekStart.localeCompare(b.weekStart));

  return (
    <div className={`musicOuter runningOuter${isEditing ? ' musicOuter--editing' : ''}`}>
      {isEditing && (
        <div className="editBanner">
          <span className="editBannerLabel">EDIT MODE</span>
          <button className="editBannerDone" onClick={exitEdit}>Done</button>
        </div>
      )}

      {/* Sticky chart area */}
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

      {/* Scrollable content */}
      <div className="runningScrollArea">
        <div className="musicPage" style={{ paddingTop: 32 }}>
          {saveError && <p className="saveError">{saveError}</p>}

          {/* Year filter + summary */}
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
                      isEditing={isEditing} onDeleteEntry={deleteEntry} />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      <nav className="bottomNav">
        <button className={`bottomNavBtn bottomNavBtn--add${isEditing ? ' bottomNavBtn--active' : ''}`}
          onClick={handleAddNew}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          <span>Add Run</span>
        </button>
      </nav>

      {showPass && (
        <PasswordModal onSubmit={enterEdit} onCancel={() => { setShowPass(false); setPending(false); }} />
      )}
      {showAdd && (
        <AddRunModal onSave={saveRun} onCancel={() => setShowAdd(false)} />
      )}
    </div>
  );
};

export default Running;
