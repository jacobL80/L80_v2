import React, { useState, useMemo, useRef, useEffect } from 'react';
import { Tooltip } from 'react-tooltip';
import 'react-tooltip/dist/react-tooltip.css';
import TruncText from './TruncText';
import '../css/ReleaseTimeline.css';

const MONTHS_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

const ACCENT = '#1696b6';

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

const ConcertTimeline = ({ history }) => {
  const [tooltip, setTooltip] = useState(null);
  const [pinned,  setPinned]  = useState(null);
  const wrapRef   = useRef(null);
  const scrollRef = useRef(null);
  const [wrapWidth, setWrapWidth] = useState(0);
  const [showAllArtists,  setShowAllArtists]  = useState(false);
  const [showAllVenues,   setShowAllVenues]   = useState(false);
  const [showAllWiths,    setShowAllWiths]    = useState(false);

  useEffect(() => {
    if (!wrapRef.current) return;
    const obs = new ResizeObserver(([e]) => setWrapWidth(e.contentRect.width));
    obs.observe(wrapRef.current);
    return () => obs.disconnect();
  }, []);

  const entries = useMemo(() =>
    history
      .map(c => ({ ...c, _date: parseDate(c.date) }))
      .filter(c => c._date)
      .sort((a, b) => a._date - b._date),
    [history]
  );

  const { minYear, maxYear } = useMemo(() => {
    if (!entries.length) return { minYear: new Date().getFullYear() - 1, maxYear: new Date().getFullYear() };
    const ys = entries.map(c => c._date.getFullYear());
    return { minYear: Math.min(...ys), maxYear: Math.max(Math.max(...ys), new Date().getFullYear()) };
  }, [entries]);

  useEffect(() => {
    if (!scrollRef.current || !entries.length) return;
    const now = new Date();
    const x = PAD_L + (now.getFullYear() - minYear) * 12 * PX_PER_MONTH;
    scrollRef.current.scrollLeft = Math.max(0, x - 6 * PX_PER_MONTH);
  }, [minYear, entries.length]);

  const mToX = (year, month) => PAD_L + ((year - minYear) * 12 + month) * PX_PER_MONTH + PX_PER_MONTH / 2;
  const totalMonths = (maxYear - minYear + 1) * 12;
  const svgWidth = Math.max(PAD_L + totalMonths * PX_PER_MONTH + PAD_R, wrapWidth);

  const dots = useMemo(() => {
    const byKey = {};
    entries.forEach(c => {
      const key = `${c._date.getFullYear()}-${c._date.getMonth()}`;
      if (!byKey[key]) byKey[key] = [];
      byKey[key].push(c);
    });
    const result = [];
    Object.entries(byKey).forEach(([key, items]) => {
      const [yr, mo] = key.split('-').map(Number);
      const x = mToX(yr, mo);
      items.forEach((c, i) => result.push({ ...c, x, y: TIMELINE_Y - DOT_R - i * (DOT_R * 2 + 3) }));
    });
    return result;
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entries, minYear]);

  const stats = useMemo(() => {
    if (!entries.length) return null;
    const byYear = {}, byBand = {}, byVenue = {}, byAttendee = {};
    entries.forEach(c => {
      const y = c._date.getFullYear();
      byYear[y] = (byYear[y] || 0) + 1;
      byBand[c.band] = (byBand[c.band] || 0) + 1;
      (c.additionalArtists ? c.additionalArtists.split(',').map(n => n.trim()).filter(Boolean) : [])
        .forEach(a => { byBand[a] = (byBand[a] || 0) + 1; });
      if (c.venue) byVenue[c.venue] = (byVenue[c.venue] || 0) + 1;
      (c.attendees ? c.attendees.split(',').map(n => n.trim()).filter(Boolean) : [])
        .forEach(a => { byAttendee[a] = (byAttendee[a] || 0) + 1; });
    });
    const allYears      = Object.entries(byYear).sort((a,b) => +a[0] - +b[0]);
    const topArtists    = Object.entries(byBand).sort((a,b) => b[1] - a[1]);
    const topVenuesSorted = Object.entries(byVenue).sort((a,b) => b[1] - a[1]);
    const topAttendees  = Object.entries(byAttendee).sort((a,b) => b[1] - a[1]);
    return {
      total:         entries.length,
      allYears,
      maxYrCount:    Math.max(...allYears.map(([,c]) => c)),
      topVenue:      topVenuesSorted[0],
      topArtist:     topArtists[0],
      topAttendee:   topAttendees[0],
      topArtists,
      topVenues:     topVenuesSorted,
      topAttendees,
    };
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
      <div className="tlEmptyIcon">🎫</div>
      <p className="tlEmptyMsg">No concerts logged yet.</p>
      <p className="tlEmptyHint">Add a concert with "Attended" checked to log it here.</p>
    </div>
  );

  return (
    <div className="releaseTimeline" onClick={clearAll} ref={wrapRef}>
      {stats && (
        <div className="tlStats">
          <div className="tlStat"><div className="tlStatNum">{stats.total}</div><div className="tlStatLabel">concerts</div></div>
          {stats.topVenue && (
            <div className="tlStat">
              <div className="tlStatNum tlStatNum--md">{stats.topVenue[0]}</div>
              <div className="tlStatLabel">top venue · {stats.topVenue[1]}</div>
            </div>
          )}
          {stats.topArtist && (
            <div className="tlStat">
              <div className="tlStatNum tlStatNum--md">{stats.topArtist[0]}</div>
              <div className="tlStatLabel">top artist · {stats.topArtist[1]}</div>
            </div>
          )}
          {stats.topAttendee && (
            <div className="tlStat">
              <div className="tlStatNum tlStatNum--md">{stats.topAttendee[0]}</div>
              <div className="tlStatLabel">most with · {stats.topAttendee[1]}</div>
            </div>
          )}
        </div>
      )}

      {stats && stats.allYears.length > 0 && (
        <div className="tlBarChart">
          {stats.allYears.map(([yr, count]) => (
            <div key={yr} className="tlBarItem">
              <div className="tlBarCount">{count}</div>
              <div className="tlBarTrack">
                <div className="tlBar" style={{ height: `${Math.round((count / stats.maxYrCount) * 52)}px`, background: '#1696b6' }} />
              </div>
              <div className="tlBarLabel">{yr}</div>
            </div>
          ))}
        </div>
      )}

      {stats && (() => {
        const PREVIEW = 6;
        const hbars = (items, color) => {
          const max = items[0][1];
          return items.map(([name, count]) => (
            <div key={name} className="tlHBarItem">
              <TruncText className="tlHBarLabel" tipId="ct-hbar-tip" content={name}>{name}</TruncText>
              <div className="tlHBarTrack">
                <div className="tlHBar" style={{ width: `${Math.round((count / max) * 100)}%`, background: color }} />
              </div>
              <div className="tlHBarCount">{count}</div>
            </div>
          ));
        };
        const toggleBtn = (showAll, setShowAll, total) => total > PREVIEW && (
          <button className="tlShowMoreBtn" onClick={() => setShowAll(v => !v)}>
            {showAll ? 'Show less' : `Show all ${total}`}
          </button>
        );
        return (
          <>
            {stats.topArtists.length > 0 && (
              <div className="tlHBarSection">
                <div className="tlHBarTitle">By Artist</div>
                {hbars(showAllArtists ? stats.topArtists : stats.topArtists.slice(0, PREVIEW), '#1696b6')}
                {toggleBtn(showAllArtists, setShowAllArtists, stats.topArtists.length)}
              </div>
            )}
            {stats.topVenues.length > 0 && (
              <div className="tlHBarSection">
                <div className="tlHBarTitle">By Venue</div>
                {hbars(showAllVenues ? stats.topVenues : stats.topVenues.slice(0, PREVIEW), '#1696b6')}
                {toggleBtn(showAllVenues, setShowAllVenues, stats.topVenues.length)}
              </div>
            )}
            {stats.topAttendees.length > 0 && (
              <div className="tlHBarSection">
                <div className="tlHBarTitle">Most With</div>
                {hbars(showAllWiths ? stats.topAttendees : stats.topAttendees.slice(0, PREVIEW), ACCENT)}
                {toggleBtn(showAllWiths, setShowAllWiths, stats.topAttendees.length)}
              </div>
            )}
          </>
        );
      })()}

      <div className="tlScrollWrap" ref={scrollRef}>
        <svg width={svgWidth} height={SVG_HEIGHT} className="tlSvg">
          {yearTicks.map(({ year, x }, i) => i % 2 === 0 ? null : (
            <rect key={year} x={x} y={0} width={12 * PX_PER_MONTH} height={SVG_HEIGHT - 20} fill="#f0f8fb" opacity="0.6" />
          ))}
          {yearTicks.map(({ year, x }) => (
            <line key={year} x1={x} y1={10} x2={x} y2={TIMELINE_Y} stroke="#cce8f0" strokeWidth="1" />
          ))}
          <line x1={PAD_L} y1={TIMELINE_Y} x2={svgWidth - PAD_R} y2={TIMELINE_Y} stroke="#8ec9d8" strokeWidth="1.5" />
          {yearTicks.map(({ year, x }) => (
            <text key={year} x={x + 5} y={SVG_HEIGHT - 4} fontSize="11" fontWeight="700" fill="#7ab8c8" fontFamily="Calibri, sans-serif">{year}</text>
          ))}
          {(() => {
            const now = new Date();
            const x = mToX(now.getFullYear(), now.getMonth());
            if (x < PAD_L || x > svgWidth - PAD_R) return null;
            return (
              <g>
                <line x1={x} y1={16} x2={x} y2={TIMELINE_Y} stroke="#1696b6" strokeWidth="1.5" strokeDasharray="4 3" opacity="0.3" />
                <text x={x + 3} y={27} fontSize="9" fontWeight="700" fill="#1696b6" opacity="0.5" fontFamily="Calibri, sans-serif">NOW</text>
              </g>
            );
          })()}
          {dots.map(dot => (
            <g key={dot.id} className="tlDotGroup" transform={`translate(${dot.x}, ${dot.y})`}
              onMouseEnter={(e) => handleEnter(e, dot)}
              onMouseMove={(e) => !pinned && setTooltip(prev => prev ? { ...prev, x: e.clientX, y: e.clientY } : prev)}
              onMouseLeave={handleLeave}
              onClick={(e) => handleClick(e, dot)}>
              <circle className="tlDot" cx={0} cy={0} r={DOT_R}
                fill={ACCENT}
                stroke={activeDot?.id === dot.id ? '#1a1a1a' : '#fff'}
                strokeWidth={activeDot?.id === dot.id ? 2.5 : 2} />
            </g>
          ))}
        </svg>
      </div>

      <Tooltip id="ct-hbar-tip" />

      {tooltip && tipPos && (
        <div className={`tlTooltip${pinned ? ' tlTooltip--pinned' : ''}`}
          style={{ ...tipPos, position: 'fixed' }}
          onClick={(e) => e.stopPropagation()}>
          <div className="tlTipArtist" style={{ color: ACCENT }}>{tooltip.dot.band}</div>
          {tooltip.dot.additionalArtists && <div className="tlTipAlbum" style={{ opacity: 0.75 }}>w/ {tooltip.dot.additionalArtists}</div>}
          {tooltip.dot.tourName && <div className="tlTipAlbum">{tooltip.dot.tourName}</div>}
          {tooltip.dot.venue && <div className="tlTipAlbum">{tooltip.dot.venue}</div>}
          {tooltip.dot.date && <div className="tlTipDate">{fmtDate(tooltip.dot.date)}</div>}
          {pinned && <button className="tlTipClose" onClick={clearAll}>×</button>}
        </div>
      )}
    </div>
  );
};

export default ConcertTimeline;
