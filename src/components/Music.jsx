import React, { useState, useEffect } from 'react';
import '../css/Music.css';

const API_URL = '/api/artists.php';

const EMPTY_ARTIST = {
  name: '', lastRelease: '', nextRelease: '', albumTitle: '',
  confirmed: false, incompleteCollection: false, notes: '', url: '',
};

const MONTHS = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];

const COOKIE = 'musicEditToken';
const COOKIE_DAYS = 14;
const getCookie  = () => { const m = document.cookie.match(new RegExp('(?:^|; )' + COOKIE + '=([^;]*)')); return m ? decodeURIComponent(m[1]) : null; };
const setCookie  = (v) => { document.cookie = `${COOKIE}=${encodeURIComponent(v)}; max-age=${COOKIE_DAYS * 86400}; path=/; SameSite=Strict`; };
const delCookie  = ()  => { document.cookie = `${COOKIE}=; max-age=0; path=/; SameSite=Strict`; };

const sortKey     = (name) => name.replace(/^The\s+/i, '');
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

  return (
    <div className="modalOverlay" onClick={onCancel}>
      <div className="modalBox" onClick={e => e.stopPropagation()}>
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
            <button className="modalCancelBtn" onClick={onCancel}>Cancel</button>
            <button className="modalSaveBtn" onClick={() => onSave(form)}
              disabled={!form.name.trim()}>Save</button>
          </div>
        </div>
      </div>
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
      {editing && released && (
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
    cardYear  = artist.lastRelease || null;
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

  useEffect(() => {
    const stored = getCookie();
    if (stored) setToken(stored);
  }, []);

  const enterEditMode = (password) => {
    setToken(password);
    setCookie(password);
    setPassModal(false);
  };

  // Fetch artists from PHP API
  useEffect(() => {
    fetch(API_URL)
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => { setArtists(data); setLoading(false); })
      .catch(() => { setFetchErr(true); setLoading(false); });
  }, []);

  const saveArtist = async (form) => {
    setSaveErr('');
    const isNew = !form.id;
    const url   = isNew ? API_URL : `${API_URL}?id=${form.id}`;
    try {
      const res = await fetch(url, {
        method: isNew ? 'POST' : 'PUT',
        headers: { 'Content-Type': 'application/json', 'X-Edit-Token': editToken },
        body: JSON.stringify({ ...form, confirmed: hasFullDate(form.nextRelease || '') }),
      });
      if (res.status === 401) {
        setSaveErr('Incorrect password. Click the ✎ icon to re-authenticate.');
        setToken(null);
        delCookie();
        return;
      }
      if (!res.ok) { setSaveErr('Save failed — please try again.'); return; }
      const saved = await res.json();
      setArtists(prev => isNew ? [...prev, saved] : prev.map(a => a.id === saved.id ? saved : a));
      setEditing(null);
    } catch {
      setSaveErr('Network error — please try again.');
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
    await saveArtist({
      ...artist,
      nextRelease: '',
      albumTitle: '',
      lastRelease: new Date().getFullYear().toString(),
    });
  };

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

  const hiatusYear = new Date().getFullYear() - 10;

  const watching = artists
    .filter(a => !a.nextRelease && !(a.lastRelease && parseInt(a.lastRelease) <= hiatusYear))
    .sort((a, b) => {
      const dy = (a.lastRelease ? parseInt(a.lastRelease) : 0) - (b.lastRelease ? parseInt(b.lastRelease) : 0);
      return dy !== 0 ? dy : sortKey(a.name).localeCompare(sortKey(b.name));
    });

  const hiatus = artists
    .filter(a => !a.nextRelease && a.lastRelease && parseInt(a.lastRelease) <= hiatusYear)
    .sort((a, b) => {
      const dy = parseInt(a.lastRelease) - parseInt(b.lastRelease);
      return dy !== 0 ? dy : sortKey(a.name).localeCompare(sortKey(b.name));
    });

  const isEditing = !!editToken;

  if (loading)    return <div className="musicPage musicLoading">Loading…</div>;
  if (fetchError) return <div className="musicPage musicLoading">Could not load data.</div>;

  return (
    <div className={`musicPage${isEditing ? ' musicPage--editing' : ''}`}>

      {isEditing && (
        <div className="editBanner">
          <span className="editBannerLabel">EDIT MODE</span>
          <div className="editBannerActions">
            <button className="editBannerAdd" onClick={() => setEditing({ ...EMPTY_ARTIST })}>
              + Add Artist
            </button>
            <button className="editBannerDone" onClick={exitEditMode}>Done</button>
          </div>
        </div>
      )}

      {saveError && <p className="saveError">{saveError}</p>}

      <div className="musicHeader">
        <p className="musicEyebrow">Music</p>
        <h1 className="musicTitle">
          Release Schedule
          <button className="titleEditBtn" onClick={() => isEditing ? exitEditMode() : setPassModal(true)}
            title={isEditing ? 'Exit edit mode' : 'Edit'}>✎</button>
        </h1>
        <div className="musicHeaderRule" />
        <p className="musicLegend">
          <span className="incompleteDot">●</span> incomplete collection
        </p>
      </div>

      {upcoming.length > 0 && (
        <section className="musicSection musicSection--upcoming">
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
        <section className="musicSection musicSection--expected">
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
        <section className="musicSection musicSection--watching">
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
        <section className="musicSection musicSection--hiatus">
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

      {showPassModal && (
        <PasswordModal onSubmit={enterEditMode} onCancel={() => setPassModal(false)} />
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
