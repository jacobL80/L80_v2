import React, { useState, useEffect, useMemo } from 'react';
import '../css/Concerts.css';

const MONTHS_SHORT = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];

function expandYear(y) { return y < 100 ? (y < 50 ? 2000 + y : 1900 + y) : y; }

function parseDate(s) {
  if (!s) return null;
  const p = s.split('/').map(Number);
  const d = p.length === 3 ? new Date(expandYear(p[2]), p[0] - 1, p[1])
          : p.length === 2 ? new Date(expandYear(p[1]), p[0] - 1, 1)
          :                  new Date(expandYear(p[0]), 0, 1);
  return isNaN(d) ? null : d;
}

function fmtDate(s) {
  const p = s.split('/').map(Number);
  if (p.length === 3) return `${MONTHS_SHORT[p[0]-1]} ${p[1]}, ${expandYear(p[2])}`;
  if (p.length === 2) return `${MONTHS_SHORT[p[0]-1]} ${expandYear(p[1])}`;
  return String(expandYear(p[0]));
}

// Everyone on the bill: the headliner, then any additional artists
function lineupOf(c) {
  const names = [c.band, ...(c.additionalArtists ? c.additionalArtists.split(',') : [])]
    .map(n => n.trim()).filter(Boolean);
  return names.filter((n, i) => names.findIndex(m => m.toLowerCase() === n.toLowerCase()) === i);
}

// Newest first; undated concerts sit on top while still upcoming, at the bottom once attended
const newestFirst = (a, b) => {
  const rank = (c) => c._date ? 1 : c.attended ? 2 : 0;
  return rank(a) - rank(b) || (b._date - a._date) || b.id - a.id;
};

function groupByBand(concerts) {
  const groups = new Map();
  concerts.forEach(c => {
    const lineup = lineupOf(c);
    const _date  = parseDate(c.date);
    lineup.forEach(name => {
      const key = name.toLowerCase();
      if (!groups.has(key)) groups.set(key, { key, name, concerts: [] });
      groups.get(key).concerts.push({ ...c, _date, _with: lineup.filter(n => n.toLowerCase() !== key) });
    });
  });
  return [...groups.values()].map(g => {
    g.concerts.sort(newestFirst);
    return { ...g, latest: g.concerts.find(c => c._date) || null };
  });
}

const sortKey = (name) => name.replace(/^(The|A)\s+/i, '');
const byName  = (a, b) => sortKey(a.name).localeCompare(sortKey(b.name));
const byCount = (dir) => (a, b) => dir * (a.concerts.length - b.concerts.length) || byName(a, b);
// Bands with no dated concert stay at the bottom in both directions
const byLatest = (dir) => (a, b) => {
  if (!a.latest || !b.latest) return (a.latest ? 0 : 1) - (b.latest ? 0 : 1) || byName(a, b);
  return dir * (a.latest._date - b.latest._date) || byName(a, b);
};

const SORTS = [
  { id: 'name-asc',    label: 'A–Z',             compare: byName },
  { id: 'name-desc',   label: 'Z–A',             compare: (a, b) => byName(b, a) },
  { id: 'recent-desc', label: 'Most recent',     compare: byLatest(-1) },
  { id: 'recent-asc',  label: 'Least recent',    compare: byLatest(1) },
  { id: 'count-desc',  label: 'Most concerts',   compare: byCount(-1) },
  { id: 'count-asc',   label: 'Fewest concerts', compare: byCount(1) },
];

// The chosen sort is remembered in a cookie, like the theme
const SORT_COOKIE = 'concertBandSort';
const readSort    = () => {
  const m = document.cookie.match(new RegExp('(?:^|; )' + SORT_COOKIE + '=([^;]*)'));
  return SORTS.some(s => s.id === m?.[1]) ? m[1] : SORTS[0].id;
};
const writeSort   = (id) => { document.cookie = `${SORT_COOKIE}=${id}; max-age=${365 * 86400}; path=/; SameSite=Lax`; };

const ChevronIcon = () => (
  <svg className="bandChevron" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <polyline points="6 9 12 15 18 9"/>
  </svg>
);

// ─── Band row ─────────────────────────────────────────────────────────────────

const BandItem = ({ band, open, onToggle }) => {
  const count = band.concerts.length;
  return (
    <div className={`bandItem${open ? ' bandItem--open' : ''}`}>
      <button type="button" className="bandHeader" aria-expanded={open} onClick={onToggle}>
        <span className="bandHeaderInfo">
          <span className="bandName">{band.name}</span>
          {band.latest && <span className="bandLatest">Latest · {fmtDate(band.latest.date)}</span>}
        </span>
        <span className="bandCount">{count} {count === 1 ? 'concert' : 'concerts'}</span>
        <ChevronIcon />
      </button>
      {open && (
        <ul className="bandConcerts">
          {band.concerts.map(c => (
            <li key={c.id} className="bandConcert">
              <div className="bandConcertDate">{c._date ? fmtDate(c.date) : (c.date || 'No date')}</div>
              <div className="bandConcertInfo">
                {c.venue    && <div>{c.venue}</div>}
                {c.tourName && <div className="bandConcertTour">{c.tourName}</div>}
                {c._with.length > 0 && <div className="bandConcertWith">w/ {c._with.join(', ')}</div>}
              </div>
              {!c.attended && <div className="bandConcertTag">Upcoming</div>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

// ─── Main component ───────────────────────────────────────────────────────────

const ConcertBands = ({ concerts }) => {
  const [sortId,   setSortId]   = useState(readSort);
  const [expanded, setExpanded] = useState(() => new Set());

  useEffect(() => { writeSort(sortId); }, [sortId]);

  const bands  = useMemo(() => groupByBand(concerts), [concerts]);
  const sorted = useMemo(
    () => [...bands].sort(SORTS.find(s => s.id === sortId).compare),
    [bands, sortId]
  );

  const toggle = (key) => setExpanded(prev => {
    const next = new Set(prev);
    if (!next.delete(key)) next.add(key);
    return next;
  });

  if (!bands.length) return <p className="allEmpty">No concerts yet — tap Add New to get started.</p>;

  return (
    <section className="musicSection bandsSection">
      <div className="bandsToolbar">
        <h2 className="musicSectionTitle">Bands ({bands.length})</h2>
        <label className="bandSort">
          <span className="bandSortLabel">Sort</span>
          <select className="bandSortSelect" value={sortId} onChange={e => setSortId(e.target.value)}>
            {SORTS.map(s => <option key={s.id} value={s.id}>{s.label}</option>)}
          </select>
        </label>
      </div>
      <div className="bandList">
        {sorted.map(band => (
          <BandItem key={band.key} band={band} open={expanded.has(band.key)}
            onToggle={() => toggle(band.key)} />
        ))}
      </div>
    </section>
  );
};

export default ConcertBands;
