import React, { useState, useMemo, useRef, useEffect } from 'react';
import '../css/ReleaseTimeline.css';

const MONTHS_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
const ACCENT = '#7c3aed';

function expandYear(y) { return y < 100 ? (y < 50 ? 2000 + y : 1900 + y) : y; }

function parseDate(s) {
  if (!s) return null;
  const p = s.split('/').map(Number);
  if (p.length === 3) return new Date(expandYear(p[2]), p[0] - 1, p[1]);
  if (p.length === 2) return new Date(expandYear(p[1]), p[0] - 1, 15);
  return new Date(expandYear(p[0]), 6, 1);
}

function fmtDate(s) {
  if (!s) return '';
  const p = s.split('/').map(Number);
  if (p.length === 3) return `${MONTHS_SHORT[p[0]-1]} ${p[1]}, ${expandYear(p[2])}`;
  if (p.length === 2) return `${MONTHS_SHORT[p[0]-1]} ${expandYear(p[1])}`;
  return String(expandYear(p[0]));
}

const PX_PER_MONTH = 32;
const DOT_R        = 7;
const TIMELINE_Y   = 180;
const SVG_HEIGHT   = 215;
const PAD_L        = 24;
const PAD_R        = 48;

const TvTimeline = ({ history }) => {
  const [tooltip, setTooltip] = useState(null);
  const [pinned,  setPinned]  = useState(null);
  const wrapRef   = useRef(null);
  const scrollRef = useRef(null);
  const [wrapWidth, setWrapWidth] = useState(0);

  useEffect(() => {
    if (!wrapRef.current) return;
    const obs = new ResizeObserver(([e]) => setWrapWidth(e.contentRect.width));
    obs.observe(wrapRef.current);
    return () => obs.disconnect();
  }, []);

  const entries = useMemo(() =>
    history
      .map(s => ({ ...s, _date: parseDate(s.date) }))
      .filter(s => s._date)
      .sort((a, b) => a._date - b._date),
    [history]
  );

  const { minYear, maxYear } = useMemo(() => {
    if (!entries.length) return { minYear: new Date().getFullYear() - 1, maxYear: new Date().getFullYear() };
    const ys = entries.map(s => s._date.getFullYear());
    return { minYear: Math.min(...ys), maxYear: Math.max(Math.max(...ys), new Date().getFullYear()) };
  }, [entries]);

  useEffect(() => {
    if (!scrollRef.current || !entries.length) return;
    const now = new Date();
    const x = PAD_L + (now.getFullYear() - minYear) * 12 * PX_PER_MONTH;
    scrollRef.current.scrollLeft = Math.max(0, x - 6 * PX_PER_MONTH);
  }, [minYear, entries.length]);

  const mToX = (year, month) => PAD_L + ((year - minYear) * 12 + month) * PX_PER_MONTH + PX_PER_MONTH / 2;
  const svgWidth = Math.max(PAD_L + (maxYear - minYear + 1) * 12 * PX_PER_MONTH + PAD_R, wrapWidth);

  const dots = useMemo(() => {
    const byKey = {};
    entries.forEach(s => {
      const key = `${s._date.getFullYear()}-${s._date.getMonth()}`;
      if (!byKey[key]) byKey[key] = [];
      byKey[key].push(s);
    });
    const result = [];
    Object.entries(byKey).forEach(([key, items]) => {
      const [yr, mo] = key.split('-').map(Number);
      const x = mToX(yr, mo);
      items.forEach((s, i) => result.push({ ...s, x, y: TIMELINE_Y - DOT_R - i * (DOT_R * 2 + 3) }));
    });
    return result;
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entries, minYear]);

  const stats = useMemo(() => {
    if (!entries.length) return null;
    const byYear = {};
    entries.forEach(s => { const y = s._date.getFullYear(); byYear[y] = (byYear[y] || 0) + 1; });
    const allYears = Object.entries(byYear).sort((a,b) => +a[0] - +b[0]);
    return { total: entries.length, allYears, maxYrCount: Math.max(...allYears.map(([,c]) => c)) };
  }, [entries]);

  const yearTicks = useMemo(() => {
    const t = [];
    let y = minYear;
    while (true) {
      const x = PAD_L + (y - minYear) * 12 * PX_PER_MONTH;
      if (x > svgWidth) break;
      t.push({ year: y, x });
      y++;
    }
    return t;
  }, [minYear, svgWidth]);

  const tipPos = tooltip ? (() => {
    const W = 230;
    const left = tooltip.x + 16 + W > window.innerWidth ? tooltip.x - W - 8 : tooltip.x + 16;
    return { left: Math.max(8, left), top: Math.max(8, tooltip.y - 90) };
  })() : null;

  const activeDot = pinned || tooltip?.dot;
  const clearAll = () => { setPinned(null); setTooltip(null); };

  if (!entries.length) return (
    <div className="tlEmpty">
      <div className="tlEmptyIcon">📺</div>
      <p className="tlEmptyMsg">No watch history yet.</p>
      <p className="tlEmptyHint">Click ✓ on a past-due card to log it.</p>
    </div>
  );

  return (
    <div className="releaseTimeline" onClick={clearAll} ref={wrapRef}>
      {stats && (
        <div className="tlStats">
          <div className="tlStat"><div className="tlStatNum">{stats.total}</div><div className="tlStatLabel">watched</div></div>
        </div>
      )}

      {stats && stats.allYears.length > 0 && (
        <div className="tlBarChart">
          {stats.allYears.map(([yr, count]) => (
            <div key={yr} className="tlBarItem">
              <div className="tlBarCount">{count}</div>
              <div className="tlBarTrack">
                <div className="tlBar" style={{ height: `${Math.round((count / stats.maxYrCount) * 52)}px`, background: ACCENT }} />
              </div>
              <div className="tlBarLabel">{yr}</div>
            </div>
          ))}
        </div>
      )}

      <div className="tlScrollWrap" ref={scrollRef}>
        <svg width={svgWidth} height={SVG_HEIGHT} className="tlSvg">
          {yearTicks.map(({ year, x }, i) => i % 2 === 0 ? null : (
            <rect key={year} x={x} y={0} width={12 * PX_PER_MONTH} height={SVG_HEIGHT - 20} fill="#f3f0ff" opacity="0.6" />
          ))}
          {yearTicks.map(({ year, x }) => (
            <line key={year} x1={x} y1={10} x2={x} y2={TIMELINE_Y} stroke="#d4c8f5" strokeWidth="1" />
          ))}
          <line x1={PAD_L} y1={TIMELINE_Y} x2={svgWidth - PAD_R} y2={TIMELINE_Y} stroke="#a78bfa" strokeWidth="1.5" />
          {yearTicks.map(({ year, x }) => (
            <text key={year} x={x + 5} y={SVG_HEIGHT - 4} fontSize="11" fontWeight="700" fill="#9879e4" fontFamily="Calibri, sans-serif">{year}</text>
          ))}
          {dots.map(dot => (
            <g key={dot.id} className="tlDotGroup" transform={`translate(${dot.x}, ${dot.y})`}
              onMouseEnter={(e) => setTooltip({ dot, x: e.clientX, y: e.clientY })}
              onMouseLeave={() => { if (!pinned) setTooltip(null); }}
              onClick={(e) => { e.stopPropagation(); if (pinned?.id === dot.id) clearAll(); else { setPinned(dot); setTooltip({ dot, x: e.clientX, y: e.clientY }); } }}>
              <circle className="tlDot" cx={0} cy={0} r={DOT_R}
                fill={ACCENT}
                stroke={activeDot?.id === dot.id ? '#1a1a1a' : '#fff'}
                strokeWidth={activeDot?.id === dot.id ? 2.5 : 2} />
            </g>
          ))}
        </svg>
      </div>

      {tooltip && tipPos && (
        <div className={`tlTooltip${pinned ? ' tlTooltip--pinned' : ''}`}
          style={{ ...tipPos, position: 'fixed' }} onClick={(e) => e.stopPropagation()}>
          <div className="tlTipArtist" style={{ color: ACCENT }}>{tooltip.dot.programName}</div>
          {tooltip.dot.service && <div className="tlTipAlbum">{tooltip.dot.service}</div>}
          {tooltip.dot.date && <div className="tlTipDate">{fmtDate(tooltip.dot.date)}</div>}
          {pinned && <button className="tlTipClose" onClick={clearAll}>×</button>}
        </div>
      )}
    </div>
  );
};

export default TvTimeline;
