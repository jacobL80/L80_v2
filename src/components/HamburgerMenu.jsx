import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import '../css/HamburgerMenu.css';
// musicHeaderRow styles are also in HamburgerMenu.css (shared)

const SECTIONS = [
  { label: 'All',         path: '/' },
  { label: 'Music',       path: '/music' },
  { label: 'Concerts',    path: '/concerts' },
  { label: 'TV / Movies', path: '/tv' },
  { label: 'Running',     path: '/running', divider: true },
];

const HamburgerMenu = () => {
  const [open, setOpen]   = useState(false);
  const navigate          = useNavigate();
  const location          = useLocation();
  const wrapRef           = useRef(null);

  // Close on outside click
  useEffect(() => {
    if (!open) return;
    const handler = (e) => { if (!wrapRef.current?.contains(e.target)) setOpen(false); };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  const handleSelect = (path) => {
    navigate(path);
    setOpen(false);
  };

  const isActive = (path) =>
    path === '/' ? location.pathname === '/' : location.pathname.startsWith(path);

  return (
    <div className="hmWrap" ref={wrapRef}>
      <button
        className={`hmBtn${open ? ' hmBtn--open' : ''}`}
        onClick={() => setOpen(v => !v)}
        aria-label="Navigation menu"
      >
        <span className="hmLine" />
        <span className="hmLine" />
        <span className="hmLine" />
      </button>

      {open && (
        <div className="hmMenu">
          {SECTIONS.map(s => (
            <React.Fragment key={s.path}>
              {s.divider && <div className="hmDivider" />}
              <button
                className={`hmItem${isActive(s.path) ? ' hmItem--active' : ''}`}
                onClick={() => handleSelect(s.path)}
              >
                {s.label}
              </button>
            </React.Fragment>
          ))}
        </div>
      )}
    </div>
  );
};

export default HamburgerMenu;
