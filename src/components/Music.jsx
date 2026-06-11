import React, { useState, useEffect } from 'react';
import '../css/Music.css';
import ReleaseTimeline from './ReleaseTimeline';

const PlusIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
  </svg>
);
const CalendarIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
  </svg>
);
const ClockIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round">
    <circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 15"/>
  </svg>
);
const EyeIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/>
  </svg>
);
const MoonIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
  </svg>
);
const BarChartIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/>
    <line x1="6" y1="20" x2="6" y2="14"/><line x1="2" y1="20" x2="22" y2="20"/>
  </svg>
);

const API_URL     = '/api/artists.php';
const HISTORY_URL = '/api/history.php';

const EMPTY_ARTIST = {
  name: '', lastRelease: '', nextRelease: '', albumTitle: '',
  confirmed: false, incompleteCollection: false, notes: '', url: '', hiatus: false,
};

const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

const COOKIE = 'musicEditToken';
const COOKIE_DAYS = 14;
const getCookie  = () => { const m = document.cookie.match(new RegExp('(?:^|; )' + COOKIE + '=([^;]*)')); return m ? decodeURIComponent(m[1]) : null; };
const setCookie  = (v) => { document.cookie = `${COOKIE}=${encodeURIComponent(v)}; max-age=${COOKIE_DAYS * 86400}; path=/; SameSite=Strict`; };
const delCookie  = ()  => { document.cookie = `${COOKIE}=; max-age=0; path=/; SameSite=Strict`; };

const sortKey     = (name) => name.replace(/^(The|A)\s+/i, '');
const hasFullDate = (s) => s.split('/').length === 3;
const parseDate   = (s) => {
  const p = s.split('/').map(Number);
  if (p.length === 3) return new Date(p[2], p[0] - 1, p[1]);
  if (p.length === 2) return new Date(p[1], p[0] - 1, 1);
  return new Date(p[0], 0, 1);
};
const parseParts  = (s) => {
  const p = s.split('/').map(Number);
  if (p.length === 3) return { month: MONTHS[p[0] - 1], day: p[1], year: p[2] };
  if (p.length === 2) return { month: MONTHS[p[0] - 1], day: null, year: p[1] };
  return { month: null, day: null, year: p[0] || null };
};
const getYear     = (s) => s.split('/').slice(-1)[0];

// ─── Password modal ───────────────────────────────────────────────────────────

const PasswordModal = ({ onSubmit, onCancel }) => {
  const [password, setPassword] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (password.trim()) onSubmit(password.trim());
  };

  return (
    <div className="modalOverlay" onClick={onCancel}>
      <div className="modalBox modalBox--narrow" onClick={e => e.stopPropagation()}>
        <h3 className="modalTitle">Edit Mode</h3>
        <form onSubmit={handleSubmit}>
          <label className="modalLabel">Password</label>
          <input className="modalInput" type="password" value={password}
            onChange={e => setPassword(e.target.value)} autoFocus />
          <div className="modalActions">
            <div className="modalActionsRight">
              <button type="button" className="modalCancelBtn" onClick={onCancel}>Cancel</button>
              <button type="submit" className="modalSaveBtn" disabled={!password.trim()}>Enter</button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

// ─── Artist form modal ────────────────────────────────────────────────────────

const ArtistForm = ({ artist, onSave, onDelete, onCancel, allArtists }) => {
  const [form, setForm]           = useState({ ...EMPTY_ARTIST, ...artist });
  const [confirmDel, setDel]      = useState(false);
  const [suggestions, setSuggestions] = useState([]);
  const wasNewOnOpen = !artist.id;
  const isNew = !form.id;
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleNameChange = (value) => {
    if (wasNewOnOpen && form.id) {
      // User is typing over a previously-selected suggestion — start fresh
      setForm({ ...EMPTY_ARTIST, name: value });
      const q = value.toLowerCase();
      setSuggestions(value.trim() ? allArtists.filter(a => a.name.toLowerCase().includes(q)).slice(0, 6) : []);
      return;
    }
    set('name', value);
    if (wasNewOnOpen && value.trim()) {
      const q = value.toLowerCase();
      setSuggestions(allArtists.filter(a => a.name.toLowerCase().includes(q)).slice(0, 6));
    } else {
      setSuggestions([]);
    }
  };

  const selectSuggestion = (a) => {
    setForm({ ...EMPTY_ARTIST, ...a });
    setSuggestions([]);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (form.name.trim()) onSave(form);
  };

  return (
    <div className="modalOverlay" onClick={onCancel}>
      <form className="modalBox" onClick={e => e.stopPropagation()} onSubmit={handleSubmit}>
        <h3 className="modalTitle">{isNew ? 'Add Artist' : 'Edit Artist'}</h3>

        <label className="modalLabel">Name</label>
        <div className="autocompleteWrap">
          <input className="modalInput" value={form.name}
            onChange={e => handleNameChange(e.target.value)}
            onBlur={() => setTimeout(() => setSuggestions([]), 150)}
            autoFocus />
          {suggestions.length > 0 && (
            <div className="autocompleteDrop">
              {suggestions.map(a => (
                <button key={a.id} className="autocompleteItem" onClick={() => selectSuggestion(a)}>
                  {a.name}
                </button>
              ))}
            </div>
          )}
        </div>

        <label className="modalLabel">Album Title</label>
        <input className="modalInput" value={form.albumTitle}
          onChange={e => set('albumTitle', e.target.value)} placeholder="leave blank if unknown" />

        <div className="modalRow">
          <div className="modalCol">
            <label className="modalLabel">Next Release</label>
            <input className="modalInput" value={form.nextRelease}
              onChange={e => set('nextRelease', e.target.value)} placeholder="YYYY · M/YYYY · M/D/YYYY" />
          </div>
          <div className="modalCol">
            <label className="modalLabel">Last Release</label>
            <input className="modalInput" value={form.lastRelease}
              onChange={e => set('lastRelease', e.target.value)} placeholder="YYYY" />
          </div>
        </div>

        <div className="modalChecks">
          <label className="modalCheckLabel">
            <input type="checkbox" checked={form.incompleteCollection}
              onChange={e => set('incompleteCollection', e.target.checked)} />
            Incomplete collection
          </label>
          <label className="modalCheckLabel">
            <input type="checkbox" checked={form.hiatus || false}
              onChange={e => set('hiatus', e.target.checked)} />
            Hiatus
          </label>
        </div>

        <label className="modalLabel">Notes</label>
        <input className="modalInput" value={form.notes}
          onChange={e => set('notes', e.target.value)} placeholder="optional" />

        <label className="modalLabel">URL</label>
        <input className="modalInput" value={form.url || ''}
          onChange={e => set('url', e.target.value)} placeholder="https://..." />

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
            <button type="submit" className="modalSaveBtn"
              disabled={!form.name.trim()}>Save</button>
          </div>
        </div>
      </form>
    </div>
  );
};

// ─── Upcoming card ────────────────────────────────────────────────────────────

const UpcomingCard = ({ artist, onEdit, onAcquire, editing }) => {
  const { month, day, year } = parseParts(artist.nextRelease);
  const releaseDate = parseDate(artist.nextRelease);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const daysUntil = Math.round((releaseDate - today) / (1000 * 60 * 60 * 24));
  const imminent = daysUntil >= 0 && daysUntil < 7;
  const released = daysUntil < 0;

  return (
    <div className={`upcomingCard${imminent ? ' upcomingCard--imminent' : ''}${released ? ' upcomingCard--released' : ''}`}>
      <div className="upcomingCardDate">
        {month && <div className="upcomingCardMonth">{month}</div>}
        <div className="upcomingCardDay">{day}</div>
        <div className="upcomingCardYear">{year}</div>
      </div>
      <div className="upcomingCardDivider" />
      <div className="upcomingCardInfo">
        <div className="upcomingCardArtist">
          {artist.url ? (
            <a href={artist.url} target="_blank" rel="noopener noreferrer" className="artistLink">
              {artist.name}<span className="externalIcon">↗</span>
            </a>
          ) : artist.name}
          {artist.incompleteCollection && (
            <span className="incompleteDot" title="Incomplete collection"> ●</span>
          )}
        </div>
        {artist.albumTitle && <div className="upcomingCardAlbum">{artist.albumTitle}</div>}
        {artist.lastRelease && <div className="cardLastRelease">Last: {artist.lastRelease}</div>}
        {imminent && (
          <div className="upcomingCardImminent">
            {daysUntil === 0 ? 'Today' : daysUntil === 1 ? 'Tomorrow' : `${daysUntil} days away`}
          </div>
        )}
      </div>
      {editing && (released || imminent) && (
        <button className="cardAcquireBtn" onClick={() => onAcquire(artist)} title="Mark as acquired">✓</button>
      )}
      {editing && (
        <button className="cardEditBtn" onClick={() => onEdit(artist)} title="Edit">✎</button>
      )}
    </div>
  );
};

// ─── Table row ────────────────────────────────────────────────────────────────

const ArtistRow = ({ artist, dateDisplay, onEdit, editing }) => {
  const albumEmpty = !artist.albumTitle && !artist.notes;
  const dateEmpty  = dateDisplay === '—';
  let cardMonth = null, cardYear = null, cardLabel = null;
  if (dateEmpty) {
    cardYear  = artist.lastRelease ? getYear(artist.lastRelease) : null;
    cardLabel = cardYear ? 'LAST' : null;
  } else if (artist.nextRelease) {
    const parts = artist.nextRelease.split('/');
    if (parts.length === 2) {
      cardMonth = MONTHS[Number(parts[0]) - 1] || null;
      cardYear  = parts[1];
    } else {
      cardYear = parts[0];
    }
  }

  const nameNode = artist.url ? (
    <a href={artist.url} target="_blank" rel="noopener noreferrer" className="artistLink">
      {artist.name}<span className="externalIcon">↗</span>
    </a>
  ) : artist.name;

  return (
    <div className={`musicRow${editing ? ' musicRow--editing' : ''}`}>
      {(cardYear || cardMonth) && (
        <>
          <div className="rowCardDate">
            {cardMonth && <div className="rowCardDateLabel">{cardMonth}</div>}
            {cardLabel && <div className="rowCardDateLabel rowCardDateLabel--last">{cardLabel}</div>}
            {cardYear && <div className="rowCardDateYear">{cardYear}</div>}
          </div>
          <div className="rowCardDivider" />
        </>
      )}
      <div className="rowCardInfo">
        <div className="rowCardArtist">
          {nameNode}
          {artist.incompleteCollection && (
            <span className="incompleteDot" title="Incomplete collection"> ●</span>
          )}
        </div>
        {!albumEmpty && (
          <div className="rowCardAlbum">
            {artist.albumTitle}
            {artist.notes && <span className="musicNotes"> ({artist.notes})</span>}
          </div>
        )}
        {!dateEmpty && artist.lastRelease && <div className="cardLastRelease">Last: {artist.lastRelease}</div>}
      </div>
      {editing && (
        <button className="rowEditBtn" onClick={() => onEdit(artist)} title="Edit">✎</button>
      )}
    </div>
  );
};

// ─── Main component ───────────────────────────────────────────────────────────

const Music = () => {
  const [artists,       setArtists] = useState([]);
  const [loading,       setLoading] = useState(true);
  const [fetchError,    setFetchErr] = useState(false);
  const [editToken,       setToken]      = useState(null);
  const [editingArtist,   setEditing]    = useState(null);
  const [saveError,       setSaveErr]    = useState('');
  const [showPassModal,   setPassModal]  = useState(false);
  const [pendingAdd,      setPendingAdd] = useState(false);
  const [history,         setHistory]   = useState([]);
  const [view,            setView]      = useState('schedule');

  useEffect(() => {
    const stored = getCookie();
    if (stored) setToken(stored);
  }, []);

  const enterEditMode = (password) => {
    setToken(password);
    setCookie(password);
    setPassModal(false);
    if (pendingAdd) {
      setPendingAdd(false);
      setEditing({ ...EMPTY_ARTIST });
    }
  };

  const handleAddNew = () => {
    if (editToken) {
      setEditing({ ...EMPTY_ARTIST });
    } else {
      setPendingAdd(true);
      setPassModal(true);
    }
  };

  // Fetch artists
  useEffect(() => {
    fetch(API_URL)
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => { setArtists(data); setLoading(false); })
      .catch(() => { setFetchErr(true); setLoading(false); });
  }, []);

  // Fetch history
  useEffect(() => {
    fetch(HISTORY_URL)
      .then(r => r.ok ? r.json() : [])
      .then(data => setHistory(data))
      .catch(() => {});
  }, []);

  const saveArtist = async (form) => {
    setSaveErr('');
    const isNew = !form.id;
    const url   = isNew ? API_URL : `${API_URL}?id=${form.id}`;
    const snapshot = artists;

    if (!isNew) {
      setArtists(prev => prev.map(a => a.id === form.id ? { ...a, ...form } : a));
      setEditing(null);
    }

    try {
      const res = await fetch(url, {
        method: isNew ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify({ ...form, confirmed: hasFullDate(form.nextRelease || '') }),
      });
      if (res.status === 401) {
        setSaveErr('Incorrect password — click Add New to re-authenticate.');
        setToken(null);
        delCookie();
        if (!isNew) setArtists(snapshot);
        return;
      }
      if (!res.ok) {
        setSaveErr('Save failed — please try again.');
        if (!isNew) setArtists(snapshot);
        return;
      }
      const saved = await res.json();
      setArtists(prev => isNew ? [...prev, saved] : prev.map(a => a.id === saved.id ? saved : a));
      if (isNew) setEditing(null);
    } catch {
      setSaveErr('Network error — please try again.');
      if (!isNew) setArtists(snapshot);
    }
  };

  const deleteArtist = async (id) => {
    try {
      const res = await fetch(`${API_URL}?id=${id}`, {
        method: 'DELETE',
        headers: { 'X-Edit-Token': editToken },
      });
      if (res.status === 401) {
        setSaveErr('Invalid password.');
        setToken(null);
        delCookie();
        return;
      }
      if (res.ok) { setArtists(prev => prev.filter(a => a.id !== id)); setEditing(null); }
    } catch {
      setSaveErr('Network error — please try again.');
    }
  };

  const acquireArtist = async (artist) => {
    // Optimistically add to history
    const tempEntry = {
      id:           `temp-${Date.now()}`,
      artist_name:  artist.name,
      album_title:  artist.albumTitle || '',
      release_date: artist.nextRelease,
      acquired_at:  new Date().toISOString().slice(0, 10),
    };
    setHistory(prev => [tempEntry, ...prev]);

    try {
      const res = await fetch(HISTORY_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify({
          artist_name:  artist.name,
          album_title:  artist.albumTitle || '',
          release_date: artist.nextRelease,
        }),
      });
      if (res.ok) {
        const saved = await res.json();
        setHistory(prev => prev.map(e => e.id === tempEntry.id ? saved : e));
      }
    } catch {}

    await saveArtist({
      ...artist,
      nextRelease: '',
      albumTitle: '',
      lastRelease: artist.nextRelease,
    });
  };

  const markHiatus = (artist) => saveArtist({ ...artist, hiatus: true });

  const exitEditMode = () => {
    setToken(null);
    delCookie();
  };

  const upcoming = artists
    .filter(a => a.nextRelease && hasFullDate(a.nextRelease))
    .sort((a, b) => parseDate(a.nextRelease) - parseDate(b.nextRelease));

  const expected = artists
    .filter(a => a.nextRelease && !hasFullDate(a.nextRelease))
    .sort((a, b) => {
      const dy = parseInt(getYear(a.nextRelease)) - parseInt(getYear(b.nextRelease));
      return dy !== 0 ? dy : sortKey(a.name).localeCompare(sortKey(b.name));
    });

  const hiatusYear = new Date().getFullYear() - 8;

  const watching = artists
    .filter(a => !a.nextRelease && !a.hiatus && !(a.lastRelease && parseInt(a.lastRelease) <= hiatusYear))
    .sort((a, b) => {
      const dy = (a.lastRelease ? parseDate(a.lastRelease) : new Date(0)) - (b.lastRelease ? parseDate(b.lastRelease) : new Date(0));
      return dy !== 0 ? dy : sortKey(a.name).localeCompare(sortKey(b.name));
    });

  const hiatus = artists
    .filter(a => !a.nextRelease && (a.hiatus || (a.lastRelease && parseInt(a.lastRelease) <= hiatusYear)))
    .sort((a, b) => {
      const ay = a.lastRelease ? parseInt(a.lastRelease) : 0;
      const by = b.lastRelease ? parseInt(b.lastRelease) : 0;
      return ay !== by ? ay - by : sortKey(a.name).localeCompare(sortKey(b.name));
    });

  const isEditing = !!editToken;

  const navSections = [
    upcoming.length > 0 && { id: 'section-upcoming', label: 'Upcoming', icon: <CalendarIcon /> },
    expected.length > 0 && { id: 'section-expected', label: 'Expected', icon: <ClockIcon /> },
    watching.length > 0 && { id: 'section-watching', label: 'Watching', icon: <EyeIcon /> },
    hiatus.length   > 0 && { id: 'section-hiatus',   label: 'Hiatus',   icon: <MoonIcon /> },
  ].filter(Boolean);

  const scrollTo = (id) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });

  const handleSectionNav = (id) => {
    if (view !== 'schedule') {
      setView('schedule');
      setTimeout(() => scrollTo(id), 60);
    } else {
      scrollTo(id);
    }
  };

  if (loading)    return <div className="musicOuter musicLoading">Loading…</div>;
  if (fetchError) return <div className="musicOuter musicLoading">Could not load data.</div>;

  return (
    <div className={`musicOuter${isEditing ? ' musicOuter--editing' : ''}`}>

      {isEditing && (
        <div className="editBanner">
          <span className="editBannerLabel">EDIT MODE</span>
          <button className="editBannerDone" onClick={exitEditMode}>Done</button>
        </div>
      )}

      <div className="musicHeader">
        <div className="musicHeaderInner">
          <p className="musicEyebrow">Music</p>
          <h1 className="musicTitle">
            {view === 'timeline' ? 'Release History' : 'Release Schedule'}
          </h1>
          <div className="musicHeaderRule" />
          {view === 'schedule' && (
            <p className="musicLegend">
              <span className="incompleteDot">●</span> incomplete collection
            </p>
          )}
        </div>
      </div>

      <div className="musicPage">

      {saveError && <p className="saveError">{saveError}</p>}

      {view === 'timeline' ? (
        <ReleaseTimeline history={history} />
      ) : (
        <>
          {upcoming.length > 0 && (
            <section id="section-upcoming" className="musicSection musicSection--upcoming">
              <h2 className="musicSectionTitle">Upcoming</h2>
              <div className="upcomingGrid">
                {upcoming.map(a => (
                  <UpcomingCard key={a.id} artist={a} onEdit={setEditing}
                    onAcquire={acquireArtist} editing={isEditing} />
                ))}
              </div>
            </section>
          )}

          {expected.length > 0 && (
            <section id="section-expected" className="musicSection musicSection--expected">
              <h2 className="musicSectionTitle">Expected</h2>
              <div className="rowGrid">
                {expected.map(a => (
                  <ArtistRow key={a.id} artist={a}
                    dateDisplay={`~${getYear(a.nextRelease)}`}
                    onEdit={setEditing} editing={isEditing} />
                ))}
              </div>
            </section>
          )}

          {watching.length > 0 && (
            <section id="section-watching" className="musicSection musicSection--watching">
              <h2 className="musicSectionTitle">Watching</h2>
              <div className="rowGrid">
                {watching.map(a => (
                  <ArtistRow key={a.id} artist={a}
                    dateDisplay="—"
                    onEdit={setEditing} editing={isEditing} />
                ))}
              </div>
            </section>
          )}

          {hiatus.length > 0 && (
            <section id="section-hiatus" className="musicSection musicSection--hiatus">
              <h2 className="musicSectionTitle">Hiatus</h2>
              <div className="rowGrid">
                {hiatus.map(a => (
                  <ArtistRow key={a.id} artist={a}
                    dateDisplay="—"
                    onEdit={setEditing} editing={isEditing} />
                ))}
              </div>
            </section>
          )}
        </>
      )}

      <nav className="bottomNav">
        <button className={`bottomNavBtn bottomNavBtn--add${isEditing ? ' bottomNavBtn--active' : ''}`}
          onClick={handleAddNew}>
          <PlusIcon />
          <span>Add New</span>
        </button>
        {navSections.length > 0 && <div className="bottomNavDivider" />}
        {navSections.map(s => (
          <button key={s.id} className="bottomNavBtn" onClick={() => handleSectionNav(s.id)}>
            {s.icon}
            <span>{s.label}</span>
          </button>
        ))}
        <div className="bottomNavDivider" />
        <button
          className={`bottomNavBtn${view === 'timeline' ? ' bottomNavBtn--active' : ''}`}
          onClick={() => setView(v => v === 'timeline' ? 'schedule' : 'timeline')}
        >
          <BarChartIcon />
          <span>History</span>
        </button>
      </nav>

      </div>{/* end musicPage */}

      {showPassModal && (
        <PasswordModal onSubmit={enterEditMode} onCancel={() => { setPassModal(false); setPendingAdd(false); }} />
      )}

      {editingArtist && (
        <ArtistForm
          artist={editingArtist}
          allArtists={artists}
          onSave={saveArtist}
          onDelete={deleteArtist}
          onCancel={() => { setEditing(null); setSaveErr(''); }}
        />
      )}
    </div>
  );
};

export default Music;
