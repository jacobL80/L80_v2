import React, { useState, useEffect } from 'react';
import { Tooltip } from 'react-tooltip';
import 'react-tooltip/dist/react-tooltip.css';
import TruncText from './TruncText';
import '../css/Music.css';
import '../css/AllView.css';
import HamburgerMenu from './HamburgerMenu';
import LoadingSpinner from './LoadingSpinner';

const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

const expandYear = (y) => y < 100 ? (y < 50 ? 2000 + y : 1900 + y) : y;
const parseParts = (s) => {
  const p = s.split('/').map(Number);
  return { month: MONTHS[p[0] - 1], day: p[1], year: expandYear(p[2]) };
};

const parseDate = (s) => {
  const p = s.split('/').map(Number);
  return new Date(expandYear(p[2]), p[0] - 1, p[1]);
};

const TYPE_LABELS      = { music: 'Music', concert: 'Concert', tv: 'TV / Movie' };
const TYPE_COLORS      = { music: '#ec6f00', concert: '#1696b6', tv: '#7c3aed' };
const SHOW_TYPE_COLORS = { TV: '#7c3aed', Movie: '#0ea5e9', Anime: '#f59e0b' };

const TYPE_ICONS = {
  music: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22">
      <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
    </svg>
  ),
  concert: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22">
      <path d="M22 10V6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v4c1.1 0 2 .9 2 2s-.9 2-2 2v4c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-4c-1.1 0-2-.9-2-2s.9-2 2-2zm-2-1.46c-1.19.69-2 1.99-2 3.46s.81 2.77 2 3.46V18H4v-2.54c1.19-.69 2-1.99 2-3.46 0-1.48-.8-2.77-2-3.46V6h16v2.54z"/>
    </svg>
  ),
  tv: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="22" height="22">
      <path d="M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z"/>
    </svg>
  ),
};

const DAYS_OF_WEEK = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

const MonthCalendar = ({ year, month, monthItems }) => {
  const firstDow    = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const byDay = {};
  monthItems.forEach(item => {
    const d = parseDate(item.date).getDate();
    if (!byDay[d]) byDay[d] = [];
    byDay[d].push(item);
  });

  const cells = [];
  for (let i = 0; i < firstDow; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);

  return (
    <div className="allCalMonth">
      <div className="allCalMonthName">{MONTHS[month]} {year}</div>
      <div className="allCalGrid">
        {DAYS_OF_WEEK.map((d, i) => <div key={i} className="allCalDow">{d}</div>)}
        {cells.map((day, i) => (
          <div key={i} className="allCalCell">
            {day !== null && (
              <>
                <span className={`allCalDayNum${byDay[day] ? ' allCalDayNum--event' : ''}`}>{day}</span>
                {byDay[day] && (
                  <div className="allCalDots">
                    {byDay[day].map((item, j) => (
                      <span
                        key={j}
                        className="allCalDotWrap"
                        data-tooltip-id="allview-tip"
                        data-tooltip-content={`${item.title}${item.subtitle ? ` — ${item.subtitle}` : ''} · ${TYPE_LABELS[item.type]}`}
                      >
                        <span className="allCalDot" style={{ background: TYPE_COLORS[item.type] }} />
                      </span>
                    ))}
                  </div>
                )}
              </>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

const hasFullDate = (s) => !!(s && s.split('/').length === 3);

const AllCalendar = ({ items }) => {
  const grouped = {};
  items.filter(item => hasFullDate(item.date)).forEach(item => {
    const d   = parseDate(item.date);
    const key = `${d.getFullYear()}-${String(d.getMonth()).padStart(2, '0')}`;
    if (!grouped[key]) grouped[key] = { year: d.getFullYear(), month: d.getMonth(), items: [] };
    grouped[key].items.push(item);
  });

  const months = Object.keys(grouped).sort().map(k => grouped[k]);
  if (months.length === 0) return null;

  return (
    <section className="musicSection allCalSection">
      <h2 className="musicSectionTitle">Calendar</h2>
      <div className="allCalScroll">
        {months.map(({ year, month, items: mi }) => (
          <MonthCalendar key={`${year}-${month}`} year={year} month={month} monthItems={mi} />
        ))}
      </div>
    </section>
  );
};

const AllCard = ({ item }) => {
  const { month, day, year } = parseParts(item.date);
  const releaseDate = parseDate(item.date);
  const today = new Date(); today.setHours(0, 0, 0, 0);
  const daysUntil = Math.round((releaseDate - today) / 86400000);
  const imminent  = daysUntil >= 0 && daysUntil < 7;
  const released  = daysUntil < 0;
  const color     = TYPE_COLORS[item.type];

  return (
    <div
      className={`allCard${imminent ? ' allCard--imminent' : ''}${released ? ' allCard--released' : ''} allCard--type-${item.type}`}
      style={{ borderLeftColor: color }}
    >
      <div className="allCardDate">
        <div className="allCardMonth" style={{ color }}>{month}</div>
        <div className={`allCardDay${imminent ? ' allCardDay--imminent' : ''}`} style={imminent ? { color } : undefined}>{day}</div>
        <div className="allCardYear">{year}</div>
      </div>
      <div className="allCardDivider" />
      <div className="allCardIconWrap" style={{ color }}>
        {TYPE_ICONS[item.type]}
      </div>
      <div className="allCardInfo">
        <div className="allCardTitle">
          {item.url ? (
            <a href={item.url} target="_blank" rel="noopener noreferrer" className="artistLink">
              {item.title}<span className="externalIcon">↗</span>
            </a>
          ) : item.title}
          {item.showType && SHOW_TYPE_COLORS[item.showType] && (
            <span className="tvTypeBadge" style={{ color: SHOW_TYPE_COLORS[item.showType], borderColor: SHOW_TYPE_COLORS[item.showType] }}>
              {item.showType}
            </span>
          )}
        </div>
        {item.subtitle && (
          <TruncText className="allCardSub" tipId="allview-tip" content={item.subtitle}>
            {item.subtitle}
          </TruncText>
        )}
        {imminent && (
          <div className="allCardImminent" style={{ color }}>
            {daysUntil === 0 ? 'Today' : daysUntil === 1 ? 'Tomorrow' : `${daysUntil} days away`}
          </div>
        )}
      </div>
    </div>
  );
};

const AllView = () => {
  useEffect(() => { document.title = 'My Tracking'; }, []);
  const [items,   setItems]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(false);

  useEffect(() => {
    fetch('/api/all.php', { cache: 'no-store' })
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => {
        const sorted = [...data].sort((a, b) => parseDate(a.date) - parseDate(b.date));
        setItems(sorted);
        setLoading(false);
      })
      .catch(() => { setError(true); setLoading(false); });
  }, []);

  const today    = new Date(); today.setHours(0, 0, 0, 0);
  const upcoming = items.filter(i => {
    const d = parseDate(i.date);
    return d >= today;
  });
  const past = items.filter(i => parseDate(i.date) < today);

  const countByType = (arr) => arr.reduce((acc, i) => {
    acc[i.type] = (acc[i.type] || 0) + 1;
    return acc;
  }, {});
  const upCounts = countByType(upcoming);

  if (loading) return <LoadingSpinner type="all" />;
  if (error)   return <div className="musicOuter musicLoading">Could not load data.</div>;

  return (
    <div className="musicOuter">
      <div className="musicScrollArea">
        <div className="musicHeader">
          <div className="musicHeaderInner">
            <div className="musicHeaderRow">
              <HamburgerMenu />
              <p className="musicEyebrow">All</p>
            </div>
            <div className="allTitleRow">
              <h1 className="musicTitle">Upcoming</h1>
              {upcoming.length > 0 && (
                <div className="allTypeSummary">
                  {['music','concert','tv'].map(t => upCounts[t] ? (
                    <span key={t} className="allTypeChip" style={{ color: TYPE_COLORS[t], borderColor: TYPE_COLORS[t] }}>
                      {upCounts[t]} {TYPE_LABELS[t]}{upCounts[t] > 1 ? (t === 'music' ? '' : 's') : ''}
                    </span>
                  ) : null)}
                </div>
              )}
            </div>
            <div className="musicHeaderRule" />
          </div>
        </div>

        <div className="musicPage">
          <Tooltip id="allview-tip" positionStrategy="fixed" style={{ zIndex: 9999 }} />
          {items.length > 0 && <AllCalendar items={items} />}
          {upcoming.length === 0 && past.length === 0 && (
            <p className="allEmpty">No upcoming dates yet — add some via Music, Concerts, or TV / Movies.</p>
          )}

          {upcoming.length > 0 && (
            <section className="musicSection">
              <h2 className="musicSectionTitle">Upcoming</h2>
              <div className="upcomingGrid">
                {upcoming.map((item, i) => <AllCard key={i} item={item} />)}
              </div>
            </section>
          )}

          {past.length > 0 && (
            <section className="musicSection">
              <h2 className="musicSectionTitle" style={{ color: '#aaa' }}>Recently Past</h2>
              <div className="upcomingGrid">
                {past.slice(-10).reverse().map((item, i) => <AllCard key={i} item={item} />)}
              </div>
            </section>
          )}
        </div>
      </div>
    </div>
  );
};

export default AllView;
