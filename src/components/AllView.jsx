import React, { useState, useEffect } from 'react';
import '../css/Music.css';
import '../css/AllView.css';
import HamburgerMenu from './HamburgerMenu';

const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

const parseParts = (s) => {
  const p = s.split('/').map(Number);
  return { month: MONTHS[p[0] - 1], day: p[1], year: p[2] };
};

const parseDate = (s) => {
  const p = s.split('/').map(Number);
  return new Date(p[2], p[0] - 1, p[1]);
};

const TYPE_LABELS = { music: 'Music', concert: 'Concert', tv: 'TV / Movie' };
const TYPE_COLORS = { music: '#ec6f00', concert: '#1696b6', tv: '#7c3aed' };

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
      className={`allCard${imminent ? ' allCard--imminent' : ''}${released ? ' allCard--released' : ''}`}
      style={{ borderLeftColor: color }}
    >
      <div className="allCardDate">
        <div className="allCardMonth" style={{ color }}>{month}</div>
        <div className={`allCardDay${imminent ? ' allCardDay--imminent' : ''}`}>{day}</div>
        <div className="allCardYear">{year}</div>
      </div>
      <div className="allCardDivider" />
      <div className="allCardInfo">
        <div className="allCardType" style={{ color }}>{TYPE_LABELS[item.type]}</div>
        <div className="allCardTitle">
          {item.url ? (
            <a href={item.url} target="_blank" rel="noopener noreferrer" className="artistLink">
              {item.title}<span className="externalIcon">↗</span>
            </a>
          ) : item.title}
        </div>
        {item.subtitle && <div className="allCardSub">{item.subtitle}</div>}
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
  const [items,   setItems]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(false);

  useEffect(() => {
    fetch('/api/all.php')
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => { setItems(data); setLoading(false); })
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

  if (loading) return <div className="musicOuter musicLoading">Loading…</div>;
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
            <h1 className="musicTitle">Upcoming</h1>
            <div className="musicHeaderRule" />
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
        </div>

        <div className="musicPage">
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
