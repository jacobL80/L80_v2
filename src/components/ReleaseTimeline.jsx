import React, { useState, useMemo } from 'react';
import '../css/ReleaseTimeline.css';

// ─── Helpers ──────────────────────────────────────────────────────────────────

const MONTHS_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

function artistColor(name) {
  let h = 0;
  for (let i = 0; i < name.length; i++) h = name.charCodeAt(i) + ((h << 5) - h);
  return `hsl(${((h % 360) + 360) % 360}, 65%, 52%)`;
}

function parseRDate(s) {
  if (!s) return null;
  const p = s.split('/').map(Number);
  if (p.length === 3) return new Date(p[2], p[0] - 1, p[1]);
  if (p.length === 2) return new Date(p[1], p[0] - 1, 15);
  return new Date(p[0], 6, 1);
}

function fmtDate(s) {
  if (!s) return '';
  const p = s.split('/').map(Number);
  if (p.length === 3) return `${MONTHS_SHORT[p[0]-1]} ${p[1]}, ${p[2]}`;
  if (p.length === 2) return `${MONTHS_SHORT[p[0]-1]} ${p[1]}`;
  return String(p[0]);
}

function fmtIso(s) {
  if (!s) return '';
  const [y, m, d] = s.split('-').map(Number);
  return `${MONTHS_SHORT[m-1]} ${d}, ${y}`;
}

// ─── Layout constants ─────────────────────────────────────────────────────────

const PX_PER_MONTH = 32;
const DOT_R        = 7;
const TIMELINE_Y   = 180;
const SVG_HEIGHT   = 215;
const PAD_L        = 24;
const PAD_R        = 48;

// ─── ReleaseTimeline ──────────────────────────────────────────────────────────

const ReleaseTimeline = ({ history }) => {
  const [tooltip, setTooltip] = useState(null); // { dot, x, y }
  const [pinned,  setPinned]  = useState(null); // pinned dot entry

  // Enriched + sorted entries
  const entries = useMemo(() =>
    history
      .map(r => ({ ...r, _date: parseRDate(r.release_date) }))
      .filter(r => r._date)
      .sort((a, b) => a._date - b._date),
    [history]
  );

  // Date range
  const { minYear, maxYear } = useMemo(() => {
    if (!entries.length) return { minYear: new Date().getFullYear() - 1, maxYear: new Date().getFullYear() };
    const ys = entries.map(r => r._date.getFullYear());
    return {
      minYear: Math.min(...ys),
      maxYear: Math.max(Math.max(...ys), new Date().getFullYear()),
    };
  }, [entries]);

  const mToX = (year, month) => PAD_L + ((year - minYear) * 12 + month) * PX_PER_MONTH + PX_PER_MONTH / 2;
  const totalMonths = (maxYear - minYear + 2) * 12;
  const svgWidth = PAD_L + totalMonths * PX_PER_MONTH + PAD_R;

  // Dot positions — stack same-month dots vertically
  const dots = useMemo(() => {
    const byKey = {};
    entries.forEach(r => {
      const key = `${r._date.getFullYear()}-${r._date.getMonth()}`;
      if (!byKey[key]) byKey[key] = [];
      byKey[key].push(r);
    });
    const result = [];
    Object.entries(byKey).forEach(([key, items]) => {
      const [yr, mo] = key.split('-').map(Number);
      const x = mToX(yr, mo);
      items.forEach((r, i) => result.push({ ...r, x, y: TIMELINE_Y - DOT_R - i * (DOT_R * 2 + 3) }));
    });
    return result;
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entries, minYear]);

  // Statistics
  const stats = useMemo(() => {
    if (!entries.length) return null;
    const thisYear = new Date().getFullYear();
    const byArtist = {}, byYear = {}, byMonth = {};
    entries.forEach(r => {
      byArtist[r.artist_name] = (byArtist[r.artist_name] || 0) + 1;
      const y = r._date.getFullYear();
      byYear[y] = (byYear[y] || 0) + 1;
      const mk = `${MONTHS_SHORT[r._date.getMonth()]} ${y}`;
      byMonth[mk] = (byMonth[mk] || 0) + 1;
    });
    const topArtist  = Object.entries(byArtist).sort((a, b) => b[1] - a[1])[0];
    const bestYear   = Object.entries(byYear).sort((a, b) => b[1] - a[1])[0];
    const peakMonth  = Object.entries(byMonth).sort((a, b) => b[1] - a[1])[0];
    const allYears   = Object.entries(byYear).sort((a, b) => +a[0] - +b[0]);
    const maxYrCount = Math.max(...allYears.map(([, c]) => c));
    let gap = 0, gc = 0;
    for (let i = 1; i < entries.length; i++) {
      gap += (entries[i]._date - entries[i-1]._date) / 86400000;
      gc++;
    }
    return {
      total:         entries.length,
      thisYearCount: entries.filter(r => r._date.getFullYear() === thisYear).length,
      topArtist, bestYear, peakMonth, allYears, maxYrCount,
      avgGapDays: gc ? Math.round(gap / gc) : null,
    };
  }, [entries]);

  // Year tick positions
  const yearTicks = useMemo(() => {
    const t = [];
    for (let y = minYear; y <= maxYear + 1; y++) {
      t.push({ year: y, x: PAD_L + (y - minYear) * 12 * PX_PER_MONTH });
    }
    return t;
  }, [minYear, maxYear]);

  // Tooltip position — flip left if too close to right edge
  const tipPos = tooltip ? (() => {
    const W = 230;
    const left = tooltip.x + 16 + W > window.innerWidth
      ? tooltip.x - W - 8
      : tooltip.x + 16;
    return { left: Math.max(8, left), top: Math.max(8, tooltip.y - 90) };
  })() : null;

  const activeDot = pinned || tooltip?.dot;

  const handleEnter = (e, dot) => setTooltip({ dot, x: e.clientX, y: e.clientY });
  const handleLeave = ()       => { if (!pinned) setTooltip(null); };
  const handleClick = (e, dot) => {
    e.stopPropagation();
    if (pinned?.id === dot.id) { setPinned(null); setTooltip(null); }
    else { setPinned(dot); setTooltip({ dot, x: e.clientX, y: e.clientY }); }
  };
  const clearAll = () => { setPinned(null); setTooltip(null); };

  if (!entries.length) return (
    <div className="tlEmpty">
      <div className="tlEmptyIcon">♫</div>
      <p className="tlEmptyMsg">No releases logged yet.</p>
      <p className="tlEmptyHint">Click ✓ on a past-due upcoming card to log it.</p>
    </div>
  );

  return (
    <div className="releaseTimeline" onClick={clearAll}>

      {/* Stats strip */}
      {stats && (
        <div className="tlStats">
          <div className="tlStat">
            <div className="tlStatNum">{stats.total}</div>
            <div className="tlStatLabel">logged</div>
          </div>
          <div className="tlStat">
            <div className="tlStatNum">{stats.thisYearCount}</div>
            <div className="tlStatLabel">this year</div>
          </div>
          {stats.bestYear && (
            <div className="tlStat">
              <div className="tlStatNum">{stats.bestYear[0]}</div>
              <div className="tlStatLabel">best year · {stats.bestYear[1]}</div>
            </div>
          )}
          {stats.topArtist && (
            <div className="tlStat tlStat--wide">
              <div className="tlStatNum tlStatNum--md" style={{ color: artistColor(stats.topArtist[0]) }}>
                {stats.topArtist[0]}
              </div>
              <div className="tlStatLabel">top artist · {stats.topArtist[1]}</div>
            </div>
          )}
          {stats.peakMonth && (
            <div className="tlStat">
              <div className="tlStatNum tlStatNum--md">{stats.peakMonth[0]}</div>
              <div className="tlStatLabel">peak month · {stats.peakMonth[1]}</div>
            </div>
          )}
          {stats.avgGapDays && (
            <div className="tlStat">
              <div className="tlStatNum">{stats.avgGapDays}d</div>
              <div className="tlStatLabel">avg gap</div>
            </div>
          )}
        </div>
      )}

      {/* Year bar chart */}
      {stats && stats.allYears.length > 0 && (
        <div className="tlBarChart">
          {stats.allYears.map(([yr, count]) => (
            <div key={yr} className="tlBarItem">
              <div className="tlBarCount">{count}</div>
              <div className="tlBarTrack">
                <div className="tlBar" style={{ height: `${Math.round((count / stats.maxYrCount) * 52)}px` }} />
              </div>
              <div className="tlBarLabel">{yr}</div>
            </div>
          ))}
        </div>
      )}

      {/* SVG dot timeline */}
      <div className="tlScrollWrap">
        <svg width={svgWidth} height={SVG_HEIGHT} className="tlSvg">

          {/* Alternating year bands */}
          {yearTicks.map(({ year, x }, i) => i % 2 === 0 ? null : (
            <rect key={year} x={x} y={0}
              width={12 * PX_PER_MONTH} height={SVG_HEIGHT - 20}
              fill="#f5f3f0" opacity="0.6" />
          ))}

          {/* Year grid lines */}
          {yearTicks.map(({ year, x }) => (
            <line key={year} x1={x} y1={10} x2={x} y2={TIMELINE_Y} stroke="#e8e3de" strokeWidth="1" />
          ))}

          {/* Baseline */}
          <line x1={PAD_L} y1={TIMELINE_Y} x2={svgWidth - PAD_R} y2={TIMELINE_Y} stroke="#cec9c3" strokeWidth="1.5" />

          {/* Year labels */}
          {yearTicks.map(({ year, x }) => (
            <text key={year} x={x + 5} y={SVG_HEIGHT - 4}
              fontSize="11" fontWeight="700" fill="#bfbab4"
              fontFamily="Calibri, sans-serif">{year}</text>
          ))}

          {/* Today marker */}
          {(() => {
            const now = new Date();
            const x = mToX(now.getFullYear(), now.getMonth());
            if (x < PAD_L || x > svgWidth - PAD_R) return null;
            return (
              <g>
                <line x1={x} y1={16} x2={x} y2={TIMELINE_Y}
                  stroke="#ec6f00" strokeWidth="1.5" strokeDasharray="4 3" opacity="0.3" />
                <text x={x + 3} y={27} fontSize="9" fontWeight="700"
                  fill="#ec6f00" opacity="0.45" fontFamily="Calibri, sans-serif">NOW</text>
              </g>
            );
          })()}

          {/* Release dots */}
          {dots.map(dot => (
            <g key={dot.id}
              className="tlDotGroup"
              transform={`translate(${dot.x}, ${dot.y})`}
              onMouseEnter={(e) => handleEnter(e, dot)}
              onMouseLeave={handleLeave}
              onClick={(e) => handleClick(e, dot)}
              style={{ cursor: 'pointer' }}
            >
              <circle
                cx={0} cy={0} r={DOT_R}
                fill={artistColor(dot.artist_name)}
                stroke={activeDot?.id === dot.id ? '#1a1a1a' : '#fff'}
                strokeWidth={activeDot?.id === dot.id ? 2.5 : 2}
              />
            </g>
          ))}
        </svg>
      </div>

      {/* Hover / click tooltip */}
      {tooltip && tipPos && (
        <div
          className={`tlTooltip${pinned ? ' tlTooltip--pinned' : ''}`}
          style={{ ...tipPos, position: 'fixed' }}
          onClick={(e) => e.stopPropagation()}
        >
          <div className="tlTipArtist" style={{ color: artistColor(tooltip.dot.artist_name) }}>
            {tooltip.dot.artist_name}
          </div>
          {tooltip.dot.album_title && (
            <div className="tlTipAlbum">{tooltip.dot.album_title}</div>
          )}
          <div className="tlTipDate">{fmtDate(tooltip.dot.release_date)}</div>
          {tooltip.dot.acquired_at && (
            <div className="tlTipAcquired">Logged {fmtIso(tooltip.dot.acquired_at)}</div>
          )}
          {pinned && <button className="tlTipClose" onClick={clearAll}>×</button>}
        </div>
      )}
    </div>
  );
};

export default ReleaseTimeline;
