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

const DAY_COLORS = {
  mon: '#4A6FA5', tue: '#47A025', wed: '#8A3ABA',
  thu: '#EC6F00', fri: '#D63030', sat: '#1696B6', sun: '#B8A000',
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

const RunningChart = ({ weeks }) => {
  const chartRef  = useRef(null);
  const [tip, setTip]   = useState(null); // { week, x, y }

  // Chart shows all weeks chronologically (oldest left, newest right)
  const chronoWeeks = useMemo(() => [...weeks].reverse(), [weeks]);
  const maxTotal    = useMemo(() => Math.max(...chronoWeeks.map(w => w.total), 1), [chronoWeeks]);

  const BAR_H    = 80;
  const BAR_W    = 22;
  const BAR_GAP  = 8;
  const PAD_L    = 12;
  const ITEM_W   = BAR_W + BAR_GAP;
  const SVG_H    = BAR_H + 36; // bars + label
  const svgW     = PAD_L + chronoWeeks.length * ITEM_W + 12;

  // Scroll to newest (rightmost) on mount
  useEffect(() => {
    if (chartRef.current) chartRef.current.scrollLeft = chartRef.current.scrollWidth;
  }, [weeks.length]);

  return (
    <div className="runChartWrap">
      <div className="runChartLegend">
        {DAYS.map(d => (
          <span key={d} className="runChartLegendItem">
            <span className="runChartLegendDot" style={{ background: DAY_COLORS[d] }} />
            {DAY_FULL[d]}
          </span>
        ))}
      </div>
      <div className="runChartScroll" ref={chartRef}>
        <svg width={svgW} height={SVG_H} className="runChartSvg">
          {chronoWeeks.map((week, i) => {
            const x = PAD_L + i * ITEM_W;
            let yOff = BAR_H;
            const segments = DAYS.map(day => {
              const h = week[day] > 0 ? Math.max((week[day] / maxTotal) * BAR_H, 2) : 0;
              yOff -= h;
              return { day, h, y: yOff };
            }).filter(s => s.h > 0);

            return (
              <g key={week.weekStart}
                onMouseEnter={(e) => setTip({ week, x: e.clientX, y: e.clientY })}
                onMouseLeave={() => setTip(null)}
                style={{ cursor: 'default' }}>
                {/* background */}
                <rect x={x} y={0} width={BAR_W} height={BAR_H} fill="#f5f3f0" rx="1" />
                {segments.map(({ day, h, y }) => (
                  <rect key={day} x={x} y={y} width={BAR_W} height={h}
                    fill={DAY_COLORS[day]} rx="1" />
                ))}
                <text x={x + BAR_W / 2} y={BAR_H + 14} textAnchor="middle"
                  fontSize="8" fill="#bbb" fontFamily="Calibri, sans-serif">
                  {fmtWeekLabel(week.weekStart)}
                </text>
                {week.total > 0 && (
                  <text x={x + BAR_W / 2} y={Math.max((1 - week.total / maxTotal) * BAR_H - 3, 4)}
                    textAnchor="middle" fontSize="7.5" fill="#999" fontFamily="Calibri, sans-serif">
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
            {week[d] > 0 ? week[d].toFixed(1) : <span className="runDash">—</span>}
          </td>
        ))}
        <td className="runCell runCell--total">{week.total.toFixed(1)}</td>
      </tr>
      {expanded && week.entries.map(entry => (
        <tr key={entry.id} className="runRowEntry">
          <td className="runCell runCell--entryDate" colSpan={8}>{entry.date}: {entry.miles} mi</td>
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
  const [yearFilter, setYearFilter] = useState('all');

  useEffect(() => { const t = getCookie(); if (t) setToken(t); }, []);

  const loadWeeks = (year = 'all') => {
    const url = year === 'all' ? API_URL : `${API_URL}?year=${year}`;
    fetch(url)
      .then(r => r.ok ? r.json() : Promise.reject())
      .then(data => { setWeeks(data); setLoading(false); })
      .catch(() => { setFetchErr(true); setLoading(false); });
  };

  useEffect(() => { loadWeeks(); }, []);

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
        {chartWeeksDesc.length > 0 && <RunningChart weeks={chartWeeksDesc} />}
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
                {yearFilter === 'all' ? 'Total' : yearFilter}: <strong>{yearTotal.toFixed(1)} mi</strong>
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
