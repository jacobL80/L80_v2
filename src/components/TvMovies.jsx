import React, { useState, useEffect } from 'react';
import '../css/Music.css';
import '../css/TvMovies.css';
import HamburgerMenu from './HamburgerMenu';
import TvTimeline from './TvTimeline';

const API_URL   = '/api/tvmovies.php';
const ACCENT    = '#7c3aed';

const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
const COOKIE      = 'musicEditToken';
const getCookie   = () => { const m = document.cookie.match(new RegExp('(?:^|; )' + COOKIE + '=([^;]*)')); return m ? decodeURIComponent(m[1]) : null; };
const setCookie   = (v) => { document.cookie = `${COOKIE}=${encodeURIComponent(v)}; max-age=${14 * 86400}; path=/; SameSite=Strict`; };
const delCookie   = ()  => { document.cookie = `${COOKIE}=; max-age=0; path=/; SameSite=Strict`; };

const hasFullDate = (s) => s.split('/').length === 3;
const parseDate   = (s) => {
  const p = s.split('/').map(Number);
  if (p.length === 3) return new Date(p[2], p[0] - 1, p[1]);
  if (p.length === 2) return new Date(p[1], p[0] - 1, 1);
  return new Date(p[0], 0, 1);
};
const parseParts = (s) => {
  const p = s.split('/').map(Number);
  if (p.length === 3) return { month: MONTHS[p[0] - 1], day: p[1], year: p[2] };
  if (p.length === 2) return { month: MONTHS[p[0] - 1], day: null, year: p[1] };
  return { month: null, day: null, year: p[0] || null };
};
const getYear = (s) => s.split('/').slice(-1)[0];

const EMPTY = { programName: '', service: '', date: '', notes: '', watched: false };

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

const ShowForm = ({ show, onSave, onDelete, onCancel }) => {
  const [form, setForm]      = useState({ ...EMPTY, ...show });
  const [confirmDel, setDel] = useState(false);
  const isNew = !form.id;
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  return (
    <div className="modalOverlay" onClick={onCancel}>
      <form className="modalBox" onClick={e => e.stopPropagation()}
        onSubmit={e => { e.preventDefault(); if (form.programName.trim()) onSave(form); }}>
        <h3 className="modalTitle">{isNew ? 'Add TV / Movie' : 'Edit TV / Movie'}</h3>

        <label className="modalLabel">Program Name</label>
        <input className="modalInput" value={form.programName}
          onChange={e => set('programName', e.target.value)} autoFocus />

        <label className="modalLabel">Streaming Service</label>
        <input className="modalInput" value={form.service}
          onChange={e => set('service', e.target.value)} placeholder="Netflix, HBO, Apple TV+…" />

        <label className="modalLabel">Date</label>
        <input className="modalInput" value={form.date}
          onChange={e => set('date', e.target.value)} placeholder="YYYY · M/YYYY · M/D/YYYY" />

        <label className="modalLabel">Notes</label>
        <input className="modalInput" value={form.notes}
          onChange={e => set('notes', e.target.value)} placeholder="optional" />

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
            <button type="button" className="modalCancelBtn" onClick={onCancel}>Cancel</button>
            <button type="submit" className="modalSaveBtn" disabled={!form.programName.trim()}>Save</button>
          </div>
        </div>
      </form>
    </div>
  );
};

// ─── Upcoming card ────────────────────────────────────────────────────────────

const ShowCard = ({ show, onEdit, onWatch, editing }) => {
  const { month, day, year } = parseParts(show.date);
  const d         = parseDate(show.date);
  const today     = new Date(); today.setHours(0, 0, 0, 0);
  const daysUntil = Math.round((d - today) / 86400000);
  const imminent  = daysUntil >= 0 && daysUntil < 7;
  const released  = daysUntil < 0;

  return (
    <div className={`upcomingCard${imminent ? ' upcomingCard--imminent' : ''}${released ? ' upcomingCard--released' : ''}`}
      style={{ borderLeftColor: ACCENT }}>
      <div className="upcomingCardDate">
        {month && <div className="upcomingCardMonth" style={{ color: ACCENT }}>{month}</div>}
        <div className="upcomingCardDay">{day}</div>
        <div className="upcomingCardYear">{year}</div>
      </div>
      <div className="upcomingCardDivider" />
      <div className="upcomingCardInfo">
        <div className="upcomingCardArtist">{show.programName}</div>
        {show.service && <div className="upcomingCardAlbum">{show.service}</div>}
        {imminent && (
          <div className="upcomingCardImminent" style={{ color: ACCENT }}>
            {daysUntil === 0 ? 'Today' : daysUntil === 1 ? 'Tomorrow' : `${daysUntil} days away`}
          </div>
        )}
      </div>
      {editing && (released || imminent) && (
        <button className="cardAcquireBtn" onClick={() => onWatch(show)} title="Mark as watched">✓</button>
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
      style={accent ? { borderLeft: `3px solid ${accent}`, background: accent === ACCENT ? '#f5f0ff' : undefined } : {}}>
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
        <div className="rowCardArtist">{show.programName}</div>
        {show.service && <div className="rowCardAlbum">{show.service}</div>}
        {show.notes   && <div className="rowCardAlbum" style={{ fontStyle: 'normal', color: '#aaa' }}>{show.notes}</div>}
      </div>
      {editing && (
        <button className="rowEditBtn" onClick={() => onEdit(show)}>✎</button>
      )}
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

// ─── Main component ───────────────────────────────────────────────────────────

const TvMovies = () => {
  const [shows,       setShows]    = useState([]);
  const [loading,     setLoading]  = useState(true);
  const [fetchError,  setFetchErr] = useState(false);
  const [editToken,   setToken]    = useState(null);
  const [editing,     setEditing]  = useState(null);
  const [saveError,   setSaveErr]  = useState('');
  const [showPass,    setShowPass] = useState(false);
  const [pendingAdd,  setPending]  = useState(false);
  const [view,        setView]     = useState('schedule');

  useEffect(() => { const t = getCookie(); if (t) setToken(t); }, []);
  useEffect(() => {
    fetch(API_URL)
      .then(r => r.ok ? r.json() : Promise.reject())
      .then(data => { setShows(data); setLoading(false); })
      .catch(() => { setFetchErr(true); setLoading(false); });
  }, []);

  const watched = shows.filter(s => s.watched);

  const upcoming = shows
    .filter(s => !s.watched && s.date && hasFullDate(s.date))
    .sort((a, b) => parseDate(a.date) - parseDate(b.date));

  const expected = shows
    .filter(s => !s.watched && s.date && !hasFullDate(s.date))
    .sort((a, b) => parseInt(getYear(a.date)) - parseInt(getYear(b.date)));

  const watchlist = shows.filter(s => !s.watched && !s.date);

  const enterEdit = (pw) => {
    setToken(pw); setCookie(pw); setShowPass(false);
    if (pendingAdd) { setPending(false); setEditing({ ...EMPTY }); }
  };
  const handleAddNew = () => {
    if (editToken) setEditing({ ...EMPTY });
    else { setPending(true); setShowPass(true); }
  };
  const exitEdit = () => { setToken(null); delCookie(); };

  const saveShow = async (form) => {
    setSaveErr('');
    const isNew = !form.id;
    const url   = isNew ? API_URL : `${API_URL}?id=${form.id}`;
    if (!isNew) { setShows(prev => prev.map(s => s.id === form.id ? { ...s, ...form } : s)); setEditing(null); }
    try {
      const res = await fetch(url, {
        method: isNew ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify(form),
      });
      if (res.status === 401) { setSaveErr('Incorrect password.'); setToken(null); delCookie(); return; }
      const saved = await res.json();
      setShows(prev => isNew ? [...prev, saved] : prev.map(s => s.id === saved.id ? saved : s));
      if (isNew) setEditing(null);
    } catch { setSaveErr('Network error.'); }
  };

  const deleteShow = async (id) => {
    try {
      const res = await fetch(`${API_URL}?id=${id}`, { method: 'DELETE', headers: { 'X-Edit-Token': editToken } });
      if (res.ok) { setShows(prev => prev.filter(s => s.id !== id)); setEditing(null); }
    } catch {}
  };

  const markWatched = (show) => saveShow({ ...show, watched: true });

  const isEditing = !!editToken;

  if (loading)    return <div className="musicOuter musicLoading">Loading…</div>;
  if (fetchError) return <div className="musicOuter musicLoading">Could not load data.</div>;

  return (
    <div className={`musicOuter${isEditing ? ' musicOuter--editing' : ''}`}>
      {isEditing && (
        <div className="editBanner">
          <span className="editBannerLabel">EDIT MODE</span>
          <button className="editBannerDone" onClick={exitEdit}>Done</button>
        </div>
      )}

      <div className="musicScrollArea">
        <div className="musicHeader" style={{ background: 'linear-gradient(135deg, rgba(124,58,237,0.06) 0%, transparent 55%)' }}>
          <div className="musicHeaderInner">
            <div className="musicHeaderRow">
              <HamburgerMenu />
              <p className="musicEyebrow" style={{ color: ACCENT }}>TV / Movies</p>
            </div>
            <h1 className="musicTitle">{view === 'history' ? 'Watch History' : 'Watch Schedule'}</h1>
            <div className="musicHeaderRule" style={{ background: ACCENT }} />
          </div>
        </div>

        <div className="musicPage">
          {saveError && <p className="saveError">{saveError}</p>}

          {view === 'history' ? (
            <TvTimeline history={watched} />
          ) : (
            <>
              {upcoming.length > 0 && (
                <section className="musicSection">
                  <h2 className="musicSectionTitle" style={{ color: ACCENT }}>Upcoming</h2>
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
                  <h2 className="musicSectionTitle" style={{ color: ACCENT }}>Expected</h2>
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
              {upcoming.length === 0 && expected.length === 0 && watchlist.length === 0 && (
                <p className="allEmpty">No shows or movies yet — click Add New to get started.</p>
              )}
            </>
          )}
        </div>
      </div>

      <nav className="bottomNav">
        <button className={`bottomNavBtn bottomNavBtn--add${isEditing ? ' bottomNavBtn--active' : ''}`}
          style={{ color: ACCENT }} onClick={handleAddNew}>
          <PlusIcon />
          <span>Add New</span>
        </button>
        <div className="bottomNavDivider" />
        <button
          className={`bottomNavBtn${view === 'history' ? ' bottomNavBtn--active' : ''}`}
          style={view === 'history' ? { color: ACCENT } : {}}
          onClick={() => setView(v => v === 'history' ? 'schedule' : 'history')}
        >
          <BarChartIcon />
          <span>History</span>
        </button>
      </nav>

      {showPass && (
        <PasswordModal onSubmit={enterEdit} onCancel={() => { setShowPass(false); setPending(false); }} />
      )}
      {editing && (
        <ShowForm show={editing} onSave={saveShow} onDelete={deleteShow}
          onCancel={() => { setEditing(null); setSaveErr(''); }} />
      )}
    </div>
  );
};

export default TvMovies;
