import React, { useState, useEffect, useRef } from 'react';
import { Tooltip } from 'react-tooltip';
import 'react-tooltip/dist/react-tooltip.css';
import '../css/Music.css';
import '../css/Concerts.css';
import HamburgerMenu from './HamburgerMenu';
import ConcertTimeline from './ConcertTimeline';

const API_URL     = '/api/concerts.php';
const ARTISTS_URL = '/api/artists.php';

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
const parseParts  = (s) => {
  const p = s.split('/').map(Number);
  if (p.length === 3) return { month: MONTHS[p[0] - 1], day: p[1], year: p[2] };
  if (p.length === 2) return { month: MONTHS[p[0] - 1], day: null, year: p[1] };
  return { month: null, day: null, year: p[0] || null };
};
const getYear = (s) => s.split('/').slice(-1)[0];

const EMPTY = { band: '', tourName: '', venue: '', date: '', notes: '', attended: false, attendees: '' };

const parseAttendees = (s) => s ? s.split(',').map(n => n.trim()).filter(Boolean) : [];
const joinAttendees  = (arr) => arr.join(', ');

const sortNonAttended = (a, b) => {
  const rank = (c) => !c.date ? 2 : hasFullDate(c.date) ? 0 : 1;
  const ra = rank(a), rb = rank(b);
  if (ra !== rb) return ra - rb;
  if (ra === 0) return parseDate(a.date) - parseDate(b.date);
  if (ra === 1) return (parseInt(getYear(a.date)) || 9999) - (parseInt(getYear(b.date)) || 9999);
  return 0;
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

// ─── Concert form modal ───────────────────────────────────────────────────────

const ConcertForm = ({ concert, onSave, onDelete, onCancel, artistNames, venueNames, attendeeNames }) => {
  const [form, setForm]               = useState({ ...EMPTY, ...concert });
  const [confirmDel, setDel]          = useState(false);
  const [bandSuggestions, setBandSug] = useState([]);
  const [bandActiveIdx, setBandActiveIdx] = useState(-1);
  const [venueSuggestions, setVenueSug] = useState([]);
  const [venueActiveIdx, setVenueActiveIdx] = useState(-1);
  const isNew = !form.id;
  const set = (k, v) => setForm(f => ({ ...f, [k]: v }));

  const handleBandChange = (value) => {
    set('band', value);
    setBandActiveIdx(-1);
    if (value.trim()) {
      const q = value.toLowerCase();
      setBandSug(artistNames.filter(n => n.toLowerCase().includes(q)).slice(0, 6));
    } else {
      setBandSug([]);
    }
  };

  const handleVenueChange = (value) => {
    set('venue', value);
    setVenueActiveIdx(-1);
    if (value.trim()) {
      const q = value.toLowerCase();
      setVenueSug(venueNames.filter(n => n.toLowerCase().includes(q)).slice(0, 6));
    } else {
      setVenueSug([]);
    }
  };

  const handleBandKeyDown = (e) => {
    if (!bandSuggestions.length) return;
    if (e.key === 'ArrowDown') { e.preventDefault(); setBandActiveIdx(i => Math.min(i + 1, bandSuggestions.length - 1)); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setBandActiveIdx(i => Math.max(i - 1, -1)); }
    else if (e.key === 'Enter' && bandActiveIdx >= 0) { e.preventDefault(); set('band', bandSuggestions[bandActiveIdx]); setBandSug([]); setBandActiveIdx(-1); }
    else if (e.key === 'Escape') { setBandSug([]); setBandActiveIdx(-1); }
  };

  const handleVenueKeyDown = (e) => {
    if (!venueSuggestions.length) return;
    if (e.key === 'ArrowDown') { e.preventDefault(); setVenueActiveIdx(i => Math.min(i + 1, venueSuggestions.length - 1)); }
    else if (e.key === 'ArrowUp') { e.preventDefault(); setVenueActiveIdx(i => Math.max(i - 1, -1)); }
    else if (e.key === 'Enter' && venueActiveIdx >= 0) { e.preventDefault(); set('venue', venueSuggestions[venueActiveIdx]); setVenueSug([]); setVenueActiveIdx(-1); }
    else if (e.key === 'Escape') { setVenueSug([]); setVenueActiveIdx(-1); }
  };

  return (
    <div className="modalOverlay" onClick={onCancel}>
      <form className="modalBox" onClick={e => e.stopPropagation()}
        onSubmit={e => { e.preventDefault(); if (form.band.trim()) onSave(form); }}>
        <h3 className="modalTitle">{isNew ? 'Add Concert' : 'Edit Concert'}</h3>

        <label className="modalLabel">Band / Artist</label>
        <div className="autocompleteWrap">
          <input className="modalInput" value={form.band}
            onChange={e => handleBandChange(e.target.value)}
            onKeyDown={handleBandKeyDown}
            onBlur={() => setTimeout(() => { setBandSug([]); setBandActiveIdx(-1); }, 150)}
            autoFocus />
          {bandSuggestions.length > 0 && (
            <div className="autocompleteDrop">
              {bandSuggestions.map((n, i) => (
                <button key={n} className={`autocompleteItem${i === bandActiveIdx ? ' autocompleteItem--active' : ''}`}
                  onClick={() => { set('band', n); setBandSug([]); setBandActiveIdx(-1); }}>
                  {n}
                </button>
              ))}
            </div>
          )}
        </div>

        <label className="modalLabel">Tour Name</label>
        <input className="modalInput" value={form.tourName}
          onChange={e => set('tourName', e.target.value)} placeholder="optional" />

        <label className="modalLabel">Venue</label>
        <div className="autocompleteWrap">
          <input className="modalInput" value={form.venue}
            onChange={e => handleVenueChange(e.target.value)}
            onKeyDown={handleVenueKeyDown}
            onBlur={() => setTimeout(() => { setVenueSug([]); setVenueActiveIdx(-1); }, 150)} />
          {venueSuggestions.length > 0 && (
            <div className="autocompleteDrop">
              {venueSuggestions.map((n, i) => (
                <button key={n} className={`autocompleteItem${i === venueActiveIdx ? ' autocompleteItem--active' : ''}`}
                  onClick={() => { set('venue', n); setVenueSug([]); setVenueActiveIdx(-1); }}>
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

        <label className="modalLabel">Attendees</label>
        <AttendeesInput
          value={form.attendees}
          onChange={v => set('attendees', v)}
          allNames={attendeeNames}
        />

        <label className="modalCheckLabel" style={{ marginTop: 4 }}>
          <input type="checkbox" checked={form.attended}
            onChange={e => set('attended', e.target.checked)} />
          Attended
        </label>

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
            <button type="submit" className="modalSaveBtn" disabled={!form.band.trim()}>Save</button>
          </div>
        </div>
      </form>
    </div>
  );
};

// ─── Upcoming concert card ────────────────────────────────────────────────────

const ConcertCard = ({ concert, onEdit, onAttend, editing }) => {
  const { month, day, year } = concert.date ? parseParts(concert.date) : { month: null, day: null, year: null };
  const hasDate  = !!concert.date;
  const fullDate = hasDate && hasFullDate(concert.date);
  const today    = new Date(); today.setHours(0, 0, 0, 0);
  const daysUntil = fullDate ? Math.round((parseDate(concert.date) - today) / 86400000) : null;
  const imminent  = daysUntil !== null && daysUntil >= 0 && daysUntil < 7;
  const past      = daysUntil !== null && daysUntil < 0;

  return (
    <div className={`upcomingCard${imminent ? ' upcomingCard--imminent' : ''}${past ? ' upcomingCard--released' : ''}`}
      style={{ borderLeftColor: '#1696b6' }}>
      {hasDate && (
        <>
          <div className="upcomingCardDate">
            {month && <div className="upcomingCardMonth" style={{ color: '#1696b6' }}>{month}</div>}
            {day   && <div className="upcomingCardDay">{day}</div>}
            {year  && <div className="upcomingCardYear">{year}</div>}
          </div>
          <div className="upcomingCardDivider" />
        </>
      )}
      <div className="upcomingCardInfo">
        <div className="upcomingCardArtist">{concert.band}</div>
        {concert.tourName && <div className="upcomingCardAlbum">{concert.tourName}</div>}
        {concert.venue    && <div className="cardLastRelease">{concert.venue}</div>}
        {imminent && (
          <div className="upcomingCardImminent" style={{ color: '#1696b6' }}>
            {daysUntil === 0 ? 'Today' : daysUntil === 1 ? 'Tomorrow' : `${daysUntil} days away`}
          </div>
        )}
      </div>
      {editing && !concert.attended && (past || imminent) && (
        <button className="cardAcquireBtn" onClick={() => onAttend(concert)} title="Mark as attended">✓</button>
      )}
      {editing && (
        <button className="cardEditBtn" onClick={() => onEdit(concert)}>✎</button>
      )}
    </div>
  );
};

// ─── Attendees chip input ─────────────────────────────────────────────────────

const AttendeesInput = ({ value, onChange, allNames }) => {
  const chips = parseAttendees(value);
  const [inputVal, setInputVal] = useState('');
  const [suggestions, setSuggestions] = useState([]);
  const [activeIdx, setActiveIdx] = useState(-1);
  const inputRef = useRef(null);

  const addName = (name) => {
    const trimmed = name.trim();
    if (!trimmed || chips.some(c => c.toLowerCase() === trimmed.toLowerCase())) return;
    onChange(joinAttendees([...chips, trimmed]));
    setInputVal('');
    setSuggestions([]);
    setActiveIdx(-1);
  };

  const removeName = (idx) => {
    const next = chips.filter((_, i) => i !== idx);
    onChange(joinAttendees(next));
  };

  const handleKey = (e) => {
    if (e.key === 'ArrowDown' && suggestions.length) {
      e.preventDefault();
      setActiveIdx(i => Math.min(i + 1, suggestions.length - 1));
    } else if (e.key === 'ArrowUp' && suggestions.length) {
      e.preventDefault();
      setActiveIdx(i => Math.max(i - 1, -1));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (activeIdx >= 0 && suggestions.length) { addName(suggestions[activeIdx]); }
      else if (inputVal.trim()) { addName(inputVal); }
    } else if (e.key === ',' && inputVal.trim()) {
      e.preventDefault();
      addName(inputVal);
    } else if (e.key === 'Backspace' && !inputVal && chips.length) {
      removeName(chips.length - 1);
    } else if (e.key === 'Escape') {
      setSuggestions([]); setActiveIdx(-1);
    }
  };

  const handleChange = (e) => {
    const v = e.target.value.replace(/,/g, '');
    setInputVal(v);
    setActiveIdx(-1);
    if (v.trim()) {
      const q = v.toLowerCase();
      setSuggestions(
        allNames.filter(n =>
          n.toLowerCase().includes(q) &&
          !chips.some(c => c.toLowerCase() === n.toLowerCase())
        ).slice(0, 6)
      );
    } else {
      setSuggestions([]);
    }
  };

  return (
    <div className="autocompleteWrap">
      <div className="attendeeChips" onClick={() => inputRef.current?.focus()}>
        {chips.map((name, i) => (
          <span key={i} className="attendeeChip">
            {name}
            <button className="attendeeChipX" onClick={() => removeName(i)} type="button">×</button>
          </span>
        ))}
        <input
          ref={inputRef}
          className="attendeeChipInput"
          value={inputVal}
          onChange={handleChange}
          onKeyDown={handleKey}
          onBlur={() => setTimeout(() => { if (inputVal.trim()) addName(inputVal); setSuggestions([]); }, 150)}
          placeholder={chips.length === 0 ? 'Add names…' : ''}
        />
      </div>
      {suggestions.length > 0 && (
        <div className="autocompleteDrop">
          {suggestions.map((n, i) => (
            <button key={n} className={`autocompleteItem${i === activeIdx ? ' autocompleteItem--active' : ''}`} type="button"
              onClick={() => addName(n)}>
              {n}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};

// ─── Plus icon ────────────────────────────────────────────────────────────────

const PlusIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round">
    <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
  </svg>
);
const CalendarIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/>
    <line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
  </svg>
);
const BarChartIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
    <line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/>
    <line x1="6" y1="20" x2="6" y2="14"/><line x1="2" y1="20" x2="22" y2="20"/>
  </svg>
);

// ─── Main component ───────────────────────────────────────────────────────────

const Concerts = () => {
  const [concerts,      setConcerts]  = useState([]);
  const [artistNames,   setArtistNames] = useState([]);
  const [loading,       setLoading]   = useState(true);
  const [fetchError,    setFetchErr]  = useState(false);
  const [editToken,     setToken]     = useState(null);
  const [editing,       setEditing]   = useState(null);
  const [saveError,     setSaveErr]   = useState('');
  const [showPass,      setShowPass]  = useState(false);
  const [pendingAdd,    setPendingAdd]= useState(false);
  const [history,       setHistory]   = useState([]);
  const [view,          setView]      = useState('schedule'); // 'schedule' | 'history'

  useEffect(() => { const t = getCookie(); if (t) setToken(t); }, []);

  useEffect(() => {
    Promise.all([
      fetch(API_URL).then(r => r.ok ? r.json() : Promise.reject()),
      fetch(ARTISTS_URL).then(r => r.ok ? r.json() : []),
    ])
      .then(([concerts, artists]) => {
        setConcerts(concerts);
        setArtistNames(artists.map(a => a.name));
        setLoading(false);
      })
      .catch(() => { setFetchErr(true); setLoading(false); });
  }, []);

  const attendedConcerts = concerts.filter(c => c.attended);
  const upcoming = concerts.filter(c => !c.attended).sort(sortNonAttended);
  const past = [...attendedConcerts].sort((a, b) => {
    if (!a.date && !b.date) return 0;
    if (!a.date) return 1;
    if (!b.date) return -1;
    return parseDate(b.date) - parseDate(a.date);
  });
  const venueNames    = [...new Set(concerts.map(c => c.venue).filter(Boolean))].sort();
  const attendeeNames = [...new Set(concerts.flatMap(c => parseAttendees(c.attendees)))].sort();

  const enterEdit = (pw) => {
    setToken(pw); setCookie(pw); setShowPass(false);
    if (pendingAdd) { setPendingAdd(false); setEditing({ ...EMPTY }); }
  };
  const handleAddNew = () => {
    if (editToken) setEditing({ ...EMPTY });
    else { setPendingAdd(true); setShowPass(true); }
  };
  const exitEdit = () => { setToken(null); delCookie(); };

  const saveConcert = async (form) => {
    setSaveErr('');
    const isNew = !form.id;
    const url   = isNew ? API_URL : `${API_URL}?id=${form.id}`;
    if (!isNew) { setConcerts(prev => prev.map(c => c.id === form.id ? { ...c, ...form } : c)); setEditing(null); }
    try {
      const res = await fetch(url, {
        method: isNew ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify(form),
      });
      if (res.status === 401) { setSaveErr('Incorrect password.'); setToken(null); delCookie(); return; }
      const saved = await res.json();
      setConcerts(prev => isNew ? [...prev, saved] : prev.map(c => c.id === saved.id ? saved : c));
      if (isNew) setEditing(null);
    } catch { setSaveErr('Network error — please try again.'); }
  };

  const deleteConcert = async (id) => {
    try {
      const res = await fetch(`${API_URL}?id=${id}`, {
        method: 'DELETE', headers: { 'X-Edit-Token': editToken },
      });
      if (res.ok) { setConcerts(prev => prev.filter(c => c.id !== id)); setEditing(null); }
    } catch { setSaveErr('Network error.'); }
  };

  const attendConcert = async (concert) => {
    await saveConcert({ ...concert, attended: true });
  };

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
        <div className="musicHeader" style={{ background: 'linear-gradient(135deg, rgba(22,150,182,0.06) 0%, transparent 55%)' }}>
          <div className="musicHeaderInner">
            <div className="musicHeaderRow">
              <HamburgerMenu />
              <p className="musicEyebrow" style={{ color: '#1696b6' }}>Concerts</p>
            </div>
            <h1 className="musicTitle">{view === 'history' ? 'Concert History' : 'Concert Schedule'}</h1>
            <div className="musicHeaderRule" style={{ background: '#1696b6' }} />
          </div>
        </div>

        <div className="musicPage">
          {saveError && <p className="saveError">{saveError}</p>}

          {view === 'history' ? (
            <ConcertTimeline history={attendedConcerts} />
          ) : (
            <>
              {upcoming.length > 0 && (
                <section className="musicSection">
                  <h2 className="musicSectionTitle" style={{ color: '#1696b6' }}>Upcoming</h2>
                  <div className="upcomingGrid">
                    {upcoming.map(c => (
                      <ConcertCard key={c.id} concert={c} onEdit={setEditing}
                        onAttend={attendConcert} editing={isEditing} />
                    ))}
                  </div>
                </section>
              )}
              {upcoming.length === 0 && past.length === 0 && (
                <p className="allEmpty">No upcoming concerts — tap Add New to get started.</p>
              )}
              {past.length > 0 && (
                <section className="musicSection">
                  <h2 className="musicSectionTitle" style={{ color: '#1696b6' }}>Past</h2>
                  <div className="upcomingGrid">
                    {past.map(c => (
                      <ConcertCard key={c.id} concert={c} onEdit={setEditing}
                        onAttend={attendConcert} editing={isEditing} />
                    ))}
                  </div>
                </section>
              )}
            </>
          )}
        </div>
      </div>

      <nav className="bottomNav">
        <button className={`bottomNavBtn bottomNavBtn--add${isEditing ? ' bottomNavBtn--active' : ''}`}
          style={{ color: '#1696b6' }} onClick={handleAddNew}>
          <PlusIcon />
          <span>Add New</span>
        </button>
        <div className="bottomNavDivider" />
        <button
          className={`bottomNavBtn${view === 'schedule' ? ' bottomNavBtn--active' : ''}`}
          style={view === 'schedule' ? { color: '#1696b6' } : {}}
          onClick={() => setView('schedule')}
        >
          <CalendarIcon />
          <span>Schedule</span>
        </button>
        <button
          className={`bottomNavBtn${view === 'history' ? ' bottomNavBtn--active' : ''}`}
          style={view === 'history' ? { color: '#1696b6' } : {}}
          onClick={() => setView('history')}
        >
          <BarChartIcon />
          <span>History</span>
        </button>
      </nav>

      {showPass && (
        <PasswordModal onSubmit={enterEdit} onCancel={() => { setShowPass(false); setPendingAdd(false); }} />
      )}
      {editing && (
        <ConcertForm concert={editing} artistNames={artistNames} venueNames={venueNames}
          attendeeNames={attendeeNames} onSave={saveConcert} onDelete={deleteConcert}
          onCancel={() => { setEditing(null); setSaveErr(''); }} />
      )}
    </div>
  );
};

export default Concerts;
