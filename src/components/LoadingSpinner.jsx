import React from 'react';

const ICONS = {
  music: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="26" height="26">
      <path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/>
    </svg>
  ),
  concert: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="26" height="26">
      <path d="M22 10V6c0-1.1-.9-2-2-2H4c-1.1 0-2 .9-2 2v4c1.1 0 2 .9 2 2s-.9 2-2 2v4c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2v-4c-1.1 0-2-.9-2-2s.9-2 2-2zm-2-1.46c-1.19.69-2 1.99-2 3.46s.81 2.77 2 3.46V18H4v-2.54c1.19-.69 2-1.99 2-3.46 0-1.48-.8-2.77-2-3.46V6h16v2.54z"/>
    </svg>
  ),
  tv: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="26" height="26">
      <path d="M21 3H3c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h5v2h8v-2h5c1.1 0 1.99-.9 1.99-2L23 5c0-1.1-.9-2-2-2zm0 14H3V5h18v12z"/>
    </svg>
  ),
  running: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="26" height="26">
      <path d="M13.49 5.48c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-3.6 13.9l1-4.4 2.1 2v6h2v-7.5l-2.1-2 .6-3c1.3 1.5 3.3 2.5 5.5 2.5v-2c-1.9 0-3.5-1-4.3-2.4l-1-1.6c-.4-.6-1-1-1.7-1-.3 0-.5.1-.8.1l-5.2 2.2v4.7h2v-3.4l1.8-.7-1.6 8.1-4.9-1-.4 2 7 1.4z"/>
    </svg>
  ),
  all: (
    <svg viewBox="0 0 24 24" fill="currentColor" width="26" height="26">
      <path d="M4 8h4V4H4v4zm6 12h4v-4h-4v4zm-6 0h4v-4H4v4zm0-6h4v-4H4v4zm6 0h4v-4h-4v4zm6-10v4h4V4h-4zm-6 4h4V4h-4v4zm6 6h4v-4h-4v4zm0 6h4v-4h-4v4z"/>
    </svg>
  ),
};

const COLORS = {
  music:   '#ec6f00',
  concert: '#1696b6',
  tv:      '#7c3aed',
  running: '#47A025',
  all:     '#888888',
};

const R = 26;
const C = 2 * Math.PI * R;

const LoadingSpinner = ({ type = 'all' }) => {
  const color = COLORS[type] || COLORS.all;
  const icon  = ICONS[type]  || ICONS.all;

  return (
    <div className="musicOuter viewSpinnerOuter">
      <div className="viewSpinnerWrap">
        <div className="viewSpinnerRing">
          <svg className="viewSpinnerSvg" viewBox="0 0 64 64">
            <circle
              cx="32" cy="32" r={R}
              fill="none"
              stroke={color}
              strokeWidth="3"
              strokeLinecap="round"
              strokeDasharray={`${(C * 0.75).toFixed(1)} ${(C * 0.25).toFixed(1)}`}
            />
          </svg>
          <div className="viewSpinnerIcon" style={{ color }}>
            {icon}
          </div>
        </div>
      </div>
    </div>
  );
};

export default LoadingSpinner;
