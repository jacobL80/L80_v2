import React, { useState, useEffect, useMemo } from 'react';
import { Tooltip } from 'react-tooltip';
import 'react-tooltip/dist/react-tooltip.css';
import '../css/Music.css';
import '../css/TvMovies.css';
import HamburgerMenu from './HamburgerMenu';
import TvTimeline from './TvTimeline';
import LoadingSpinner from './LoadingSpinner';
import TruncText from './TruncText';

const API_URL   = '/api/tvmovies.php';
const ACCENT      = '#7c3aed';
const SHOW_TYPES  = ['TV', 'Movie', 'Anime'];
const TYPE_COLORS = { TV: '#7c3aed', Movie: '#0ea5e9', Anime: '#f59e0b' };

const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
const COOKIE      = 'musicEditToken';
const getCookie   = () => { const m = document.cookie.match(new RegExp('(?:^|; )' + COOKIE + '=([^;]*)')); return m ? decodeURIComponent(m[1]) : null; };
const setCookie   = (v) => { document.cookie = `${COOKIE}=${encodeURIComponent(v)}; max-age=${14 * 86400}; path=/; SameSite=Strict`; };
const delCookie   = ()  => { document.cookie = `${COOKIE}=; max-age=0; path=/; SameSite=Strict`; };

const expandYear  = (y) => y < 100 ? (y < 50 ? 2000 + y : 1900 + y) : y;
const hasFullDate = (s) => s.split('/').length === 3;
const parseDate   = (s) => {
  const p = s.split('/').map(Number);
  if (p.length === 3) return new Date(expandYear(p[2]), p[0] - 1, p[1]);
  if (p.length === 2) return new Date(expandYear(p[1]), p[0] - 1, 1);
  return new Date(expandYear(p[0]), 0, 1);
};
const parseParts = (s) => {
  const p = s.split('/').map(Number);
  if (p.length === 3) return { month: MONTHS[p[0] - 1], day: p[1], year: expandYear(p[2]) };
  if (p.length === 2) return { month: MONTHS[p[0] - 1], day: null, year: expandYear(p[1]) };
  return { month: null, day: null, year: p[0] ? expandYear(p[0]) : null };
};
const getYear = (s) => String(expandYear(Number(s.split('/').slice(-1)[0])));

const EMPTY = { programName: '', service: '', date: '', notes: '', type: '', watched: false };

// ─── Password modal ───────────────────────────────────────────────────────────

const PasswordModal = ({ onSubmit, onCancel }) => {
  const [pw, setPw] = useState('');
  return (
    <div className="modalOverlay" onClick={onCancel}>
      <div className="modalBox modalBox--narrow" onClick={e => e.stopPropagation()}>
        <h3 className="modalTitle">Edit Mode</h3>
        <form onSubmit={e => { e.preventDefault(); if (pw.trim()) onSubmit(pw.trim()); }}>
          <label className="modalLabel">Password</label>
          <input className="modalInput" type="password" value={pw}
            onChange={e => setPw(e.target.value)} autoFocus />
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

// ─── TV/Movie form ────────────────────────────────────────────────────────────

const ShowForm = ({ show, onSave, onDelete, onCancel, serviceNames = [] }) => {
  const [form, setForm]           = useState({ ...EMPTY, ...show });
  const [confirmDel, setDel]      = useState(false);
  const [saving, setSaving]       = useState(false);
  const [result, setResult]       = useState(null);
  const [serviceSug, setSvcSug]   = useState([]);
  const [svcActiveIdx, setSvcIdx] = useState(-1);
  const isNew = !form.id;
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleServiceChange = (value) => {
    set('service', value);
    setSvcIdx(-1);
    if (value.trim()) {
      const q = value.toLowerCase();
      setSvcSug(serviceNames.filter(n => n.toLowerCase().includes(q)).slice(0, 5));
    } else {
      setSvcSug([]);
    }
  };

  const handleServiceKeyDown = (e) => {
    if (!serviceSug.length) return;
    if (e.key === 'ArrowDown')      { e.preventDefault(); setSvcIdx(i => Math.min(i + 1, serviceSug.length - 1)); }
    else if (e.key === 'ArrowUp')   { e.preventDefault(); setSvcIdx(i => Math.max(i - 1, -1)); }
    else if (e.key === 'Enter' && svcActiveIdx >= 0) { e.preventDefault(); set('service', serviceSug[svcActiveIdx]); setSvcSug([]); setSvcIdx(-1); }
    else if (e.key === 'Escape')    { setSvcSug([]); setSvcIdx(-1); }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.programName.trim()) return;
    setSaving(true); setResult(null);
    try {
      await onSave(form);
      setResult({ ok: true });
      setTimeout(onCancel, 1500);
    } catch (err) {
      setSaving(false);
      setResult({ ok: false, message: err.message || 'Something went wrong.' });
    }
  };

  return (
    <div className="modalOverlay" onClick={saving ? undefined : onCancel}>
      <form className="modalBox" style={{ position: 'relative' }} onClick={e => e.stopPropagation()}
        onSubmit={handleSubmit}>
        <h3 className="modalTitle">{isNew ? 'Add TV / Movie' : 'Edit TV / Movie'}</h3>

        <label className="modalLabel">Program Name</label>
        <input className="modalInput" value={form.programName}
          onChange={e => set('programName', e.target.value)} autoFocus />

        <label className="modalLabel">Where to Watch</label>
        <div className="autocompleteWrap">
          <input className="modalInput" value={form.service}
            onChange={e => handleServiceChange(e.target.value)}
            onKeyDown={handleServiceKeyDown}
            onBlur={() => setTimeout(() => { setSvcSug([]); setSvcIdx(-1); }, 150)}
            placeholder="Netflix, HBO, Apple TV+…" />
          {serviceSug.length > 0 && (
            <div className="autocompleteDrop">
              {serviceSug.map((n, i) => (
                <button key={n} type="button"
                  className={`autocompleteItem${i === svcActiveIdx ? ' autocompleteItem--active' : ''}`}
                  onClick={() => { set('service', n); setSvcSug([]); setSvcIdx(-1); }}>
                  {n}
                </button>
              ))}
            </div>
          )}
        </div>

        <label className="modalLabel">Date</label>
        <input className="modalInput" value={form.date}
          onChange={e => set('date', e.target.value)} placeholder="YYYY · M/YYYY · M/D/YYYY" />

        <label className="modalLabel">Notes</label>
        <input className="modalInput" value={form.notes}
          onChange={e => set('notes', e.target.value)} placeholder="optional" />

        <label className="modalLabel">Type</label>
        <select className="modalInput" value={form.type} onChange={e => set('type', e.target.value)}>
          <option value="">— Select type —</option>
          {SHOW_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
        </select>

        <div className="modalActions">
          {!isNew && !confirmDel && (
            <button className="modalDeleteBtn" onClick={() => setDel(true)}>Delete</button>
          )}
          {!isNew && confirmDel && (
            <span className="modalDeleteConfirm">
              Sure?&nbsp;
              <button className="modalDeleteBtn" onClick={() => onDelete(form.id)}>Yes, delete</button>
              &nbsp;
              <button className="modalCancelBtn" onClick={() => setDel(false)}>No</button>
            </span>
          )}
          <div className="modalActionsRight">
            <button type="button" className="modalCancelBtn" onClick={onCancel} disabled={saving}>Cancel</button>
            <button type="submit" className="modalSaveBtn" disabled={saving || !form.programName.trim()}>
              {saving ? <><span className="btnSpinner" />Saving…</> : 'Save'}
            </button>
          </div>
        </div>
        {result && (
          <div className={`modalResultOverlay${result.ok ? ' modalResultOverlay--ok' : ' modalResultOverlay--err'}`}>
            <div className="modalResultIcon">{result.ok ? '✓' : '✗'}</div>
            <p className="modalResultMsg">{result.ok ? 'Saved!' : result.message}</p>
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

// ─── Upcoming card ────────────────────────────────────────────────────────────

const TO_WATCH_GREEN = '#16a34a';

const ShowCard = ({ show, onEdit, onWatch, editing, watchable = false }) => {
  const { month, day, year } = parseParts(show.date);
  const d         = parseDate(show.date);
  const now       = new Date(); now.setHours(0, 0, 0, 0);
  const daysUntil = Math.round((d - now) / 86400000);
  const imminent  = !watchable && daysUntil >= 0 && daysUntil < 7;

  const accent = watchable ? TO_WATCH_GREEN : ACCENT;

  return (
    <div className={`upcomingCard${watchable ? ' upcomingCard--toWatch' : imminent ? ' upcomingCard--imminent' : ''}`}
      style={watchable ? { borderLeftColor: accent, background: '#f0fdf4', boxShadow: '0 2px 10px rgba(22,163,74,0.08)' } : {}}>
      <div className="upcomingCardDate">
        {month && <div className="upcomingCardMonth" style={watchable ? { color: accent } : undefined}>{month}</div>}
        <div className="upcomingCardDay" style={watchable ? { color: accent } : undefined}>{day}</div>
        <div className="upcomingCardYear">{year}</div>
      </div>
      <div className="upcomingCardDivider" style={watchable ? { background: accent, opacity: 0.3 } : undefined} />
      <div className="upcomingCardInfo">
        <div className="tvTitleRow">
          <div className="upcomingCardArtist">{show.programName}</div>
          {show.type && <span className="tvTypeBadge" style={{ color: TYPE_COLORS[show.type], borderColor: TYPE_COLORS[show.type] }}>{show.type}</span>}
        </div>
        {show.service && (
          <TruncText className="upcomingCardAlbum" tipId="tv-tip" content={show.service}>
            {show.service}
          </TruncText>
        )}
        {imminent && (
          <div className="upcomingCardImminent">
            {daysUntil === 0 ? 'Today' : daysUntil === 1 ? 'Tomorrow' : `${daysUntil} days away`}
          </div>
        )}
      </div>
      {(watchable || (editing && imminent)) && (
        <button className="cardAcquireBtn" style={watchable ? { color: accent, borderColor: accent } : undefined}
          onClick={() => onWatch(show)} title="Mark as watched">✓</button>
      )}
      {editing && (
        <button className="cardEditBtn" onClick={() => onEdit(show)}>✎</button>
      )}
    </div>
  );
};

// ─── Row for expected / watching ─────────────────────────────────────────────

const ShowRow = ({ show, onEdit, editing, accent }) => {
  const hasDate = !!show.date;
  const parts   = hasDate ? parseParts(show.date) : null;
  const yearText  = parts ? String(parts.year) : null;
  const monthText = parts && !parts.day ? parts.month : null;

  return (
    <div className={`musicRow${editing ? ' musicRow--editing' : ''}`}
      style={accent ? { borderLeft: `3px solid ${accent}`, background: accent === ACCENT ? 'var(--accent-light)' : undefined } : {}}>
      {yearText && (
        <>
          <div className="rowCardDate">
            {monthText && <div className="rowCardDateLabel" style={{ color: accent }}>{monthText}</div>}
            <div className="rowCardDateYear" style={{ color: accent }}>{yearText}</div>
          </div>
          <div className="rowCardDivider" />
        </>
      )}
      <div className="rowCardInfo">
        <div className="tvTitleRow">
          <div className="rowCardArtist">{show.programName}</div>
          {show.type && <span className="tvTypeBadge" style={{ color: TYPE_COLORS[show.type], borderColor: TYPE_COLORS[show.type] }}>{show.type}</span>}
        </div>
        {show.service && (
          <TruncText className="rowCardAlbum" tipId="tv-tip" content={show.service}>
            {show.service}
          </TruncText>
        )}
        {show.notes && (
          <TruncText className="rowCardAlbum" style={{ fontStyle: 'normal', color: '#aaa' }}
            tipId="tv-tip" content={show.notes}>
            {show.notes}
          </TruncText>
        )}
      </div>
      {editing && (
        <button className="rowEditBtn" onClick={() => onEdit(show)}>✎</button>
      )}
    </div>
  );
};

// ─── Type breakdown ───────────────────────────────────────────────────────────

const TvTypeBreakdown = ({ watched }) => {
  const years = useMemo(() => {
    const ys = [...new Set(
      watched.map(s => s.date ? getYear(s.date) : null).filter(Boolean)
    )].sort().reverse();
    return ys;
  }, [watched]);

  const [yr, setYr] = useState(null);

  const subset = useMemo(() =>
    yr ? watched.filter(s => s.date && getYear(s.date) === yr) : watched
  , [watched, yr]);

  const total = subset.length;

  const counts = useMemo(() => {
    const out = {};
    SHOW_TYPES.forEach(t => { out[t] = subset.filter(s => s.type === t).length; });
    return out;
  }, [subset]);

  if (total === 0) return null;

  return (
    <div className="tvTypeBreak">
      <div className="tvTypeBreakHeader">
        <span className="tvTypeBreakTitle">By Type</span>
        <div className="tvTypeYearPills">
          <button className={`tvTypeYearPill${!yr ? ' tvTypeYearPill--active' : ''}`}
            onClick={() => setYr(null)}>All</button>
          {years.map(y => (
            <button key={y}
              className={`tvTypeYearPill${yr === y ? ' tvTypeYearPill--active' : ''}`}
              onClick={() => setYr(y === yr ? null : y)}>{y}</button>
          ))}
        </div>
      </div>
      {SHOW_TYPES.map(t => {
        const count = counts[t] || 0;
        const pct   = total > 0 ? Math.round(count / total * 100) : 0;
        return (
          <div key={t} className="tvTypeBarItem">
            <div className="tvTypeBarLabel">{t}</div>
            <div className="tvTypeBarTrack">
              <div className="tvTypeBar" style={{ width: `${pct}%`, background: TYPE_COLORS[t] }} />
            </div>
            <div className="tvTypeBarStat">{pct}%<span className="tvTypeBarCount"> ({count})</span></div>
          </div>
        );
      })}
    </div>
  );
};

// ─── Icons ────────────────────────────────────────────────────────────────────

const PlusIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
  </svg>
);
const BarChartIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/>
    <line x1="6" y1="20" x2="6" y2="14"/><line x1="2" y1="20" x2="22" y2="20"/>
  </svg>
);
const CalendarIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
  </svg>
);

// ─── Main component ───────────────────────────────────────────────────────────

const TvMovies = () => {
  const [shows,       setShows]    = useState([]);
  const [loading,     setLoading]  = useState(true);
  const [fetchError,  setFetchErr] = useState(false);
  const [editToken,   setToken]    = useState(null);
  const [editing,     setEditing]  = useState(null);
  const [saveError,   setSaveErr]  = useState('');
  const [showPass,    setShowPass] = useState(false);
  const [pendingAdd,   setPending]     = useState(false);
  const [pendingWatch, setPendingWatch] = useState(null);
  const [view,         setView]        = useState('schedule');

  useEffect(() => { const t = getCookie(); if (t) setToken(t); }, []);
  useEffect(() => {
    fetch(API_URL)
      .then(r => r.ok ? r.json() : Promise.reject())
      .then(data => { setShows(data); setLoading(false); })
      .catch(() => { setFetchErr(true); setLoading(false); });
  }, []);

  const watched = shows.filter(s => s.watched);

  const _today = new Date(); _today.setHours(0, 0, 0, 0);

  const toWatch = shows
    .filter(s => !s.watched && s.date && hasFullDate(s.date) && parseDate(s.date) <= _today)
    .sort((a, b) => parseDate(a.date) - parseDate(b.date));

  const upcoming = shows
    .filter(s => !s.watched && s.date && hasFullDate(s.date) && parseDate(s.date) > _today)
    .sort((a, b) => parseDate(a.date) - parseDate(b.date));

  const expected = shows
    .filter(s => !s.watched && s.date && !hasFullDate(s.date))
    .sort((a, b) => parseInt(getYear(a.date)) - parseInt(getYear(b.date)));

  const watchlist = shows.filter(s => !s.watched && !s.date);

  const enterEdit = (pw) => {
    setToken(pw); setCookie(pw); setShowPass(false);
    if (pendingAdd) { setPending(false); setEditing({ ...EMPTY }); }
    else if (pendingWatch) { const s = pendingWatch; setPendingWatch(null); saveShow({ ...s, watched: true }, pw).catch(e => setSaveErr(e.message)); }
  };
  const handleAddNew = () => {
    if (editToken) setEditing({ ...EMPTY });
    else { setPending(true); setShowPass(true); }
  };
  const exitEdit = () => { setToken(null); delCookie(); };

  const saveShow = async (form, token = editToken) => {
    const isNew = !form.id;
    const url   = isNew ? API_URL : `${API_URL}?id=${form.id}`;
    const snapshot = shows;
    if (!isNew) setShows(prev => prev.map(s => s.id === form.id ? { ...s, ...form } : s));

    let res;
    try {
      res = await fetch(url, {
        method: isNew ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': token },
        body: JSON.stringify(form),
      });
    } catch {
      if (!isNew) setShows(snapshot);
      throw new Error('Network error — please try again.');
    }

    if (res.status === 401) {
      setToken(null); delCookie();
      if (!isNew) setShows(snapshot);
      throw new Error('Incorrect password.');
    }
    const saved = await res.json();
    setShows(prev => isNew ? [...prev, saved] : prev.map(s => s.id === saved.id ? saved : s));
  };

  const deleteShow = async (id) => {
    try {
      const res = await fetch(`${API_URL}?id=${id}`, { method: 'DELETE', headers: { 'X-Edit-Token': editToken } });
      if (res.ok) { setShows(prev => prev.filter(s => s.id !== id)); setEditing(null); }
    } catch {}
  };

  const markWatched       = (show) => saveShow({ ...show, watched: true }).catch(e => setSaveErr(e.message));
  const handleMarkWatched = (show) => {
    if (editToken) markWatched(show);
    else { setPendingWatch(show); setShowPass(true); }
  };

  const isEditing    = !!editToken;
  const serviceNames = useMemo(() =>
    [...new Set(shows.map(s => s.service).filter(Boolean))].sort()
  , [shows]);

  if (loading)    return <LoadingSpinner type="tv" />;
  if (fetchError) return <div className="musicOuter musicLoading">Could not load data.</div>;

  return (
    <div className={`musicOuter tvOuter${isEditing ? ' musicOuter--editing' : ''}`}>
      {isEditing && (
        <div className="editBanner">
          <span className="editBannerLabel">EDIT MODE</span>
          <button className="editBannerDone" onClick={exitEdit}>Done</button>
        </div>
      )}

      <div className="musicScrollArea">
        <div className="musicHeader">
          <div className="musicHeaderInner">
            <div className="musicHeaderRow">
              <HamburgerMenu />
              <p className="musicEyebrow">TV / Movies</p>
            </div>
            <h1 className="musicTitle">{view === 'history' ? 'Watch History' : 'Watch Schedule'}</h1>
            <div className="musicHeaderRule" />
          </div>
        </div>

        <div className="musicPage">
          {saveError && <p className="saveError">{saveError}</p>}

          {view === 'history' ? (
            <>
              <TvTypeBreakdown watched={watched} />
              <TvTimeline history={watched} />
            </>
          ) : (
            <>
              {toWatch.length > 0 && (
                <section className="musicSection">
                  <h2 className="musicSectionTitle" style={{ color: TO_WATCH_GREEN }}>To Watch</h2>
                  <div className="upcomingGrid">
                    {toWatch.map(s => (
                      <ShowCard key={s.id} show={s} onEdit={setEditing}
                        onWatch={handleMarkWatched} editing={isEditing} watchable />
                    ))}
                  </div>
                </section>
              )}
              {upcoming.length > 0 && (
                <section className="musicSection">
                  <h2 className="musicSectionTitle">Upcoming</h2>
                  <div className="upcomingGrid">
                    {upcoming.map(s => (
                      <ShowCard key={s.id} show={s} onEdit={setEditing}
                        onWatch={markWatched} editing={isEditing} />
                    ))}
                  </div>
                </section>
              )}
              {expected.length > 0 && (
                <section className="musicSection">
                  <h2 className="musicSectionTitle">Expected</h2>
                  <div className="rowGrid">
                    {expected.map(s => (
                      <ShowRow key={s.id} show={s} onEdit={setEditing} editing={isEditing} accent={ACCENT} />
                    ))}
                  </div>
                </section>
              )}
              {watchlist.length > 0 && (
                <section className="musicSection">
                  <h2 className="musicSectionTitle" style={{ color: '#aaa' }}>Watchlist</h2>
                  <div className="rowGrid">
                    {watchlist.map(s => (
                      <ShowRow key={s.id} show={s} onEdit={setEditing} editing={isEditing} accent="#aaa" />
                    ))}
                  </div>
                </section>
              )}
              {toWatch.length === 0 && upcoming.length === 0 && expected.length === 0 && watchlist.length === 0 && (
                <p className="allEmpty">No shows or movies yet — click Add New to get started.</p>
              )}
            </>
          )}
        </div>
      </div>

      <nav className="bottomNav">
        <button className={`bottomNavBtn bottomNavBtn--add${isEditing ? ' bottomNavBtn--active' : ''}`}
          onClick={handleAddNew}>
          <PlusIcon />
          <span>Add New</span>
        </button>
        <div className="bottomNavDivider" />
        <button
          className={`bottomNavBtn${view === 'schedule' ? ' bottomNavBtn--active' : ''}`}
          onClick={() => setView('schedule')}
        >
          <CalendarIcon />
          <span>Schedule</span>
        </button>
        <button
          className={`bottomNavBtn${view === 'history' ? ' bottomNavBtn--active' : ''}`}
          onClick={() => setView('history')}
        >
          <BarChartIcon />
          <span>History</span>
        </button>
      </nav>

      <Tooltip id="tv-tip" />

      {showPass && (
        <PasswordModal onSubmit={enterEdit} onCancel={() => { setShowPass(false); setPending(false); setPendingWatch(null); }} />
      )}
      {editing && (
        <ShowForm show={editing} onSave={saveShow} onDelete={deleteShow}
          serviceNames={serviceNames}
          onCancel={() => { setEditing(null); setSaveErr(''); }} />
      )}
    </div>
  );
};

export default TvMovies;
