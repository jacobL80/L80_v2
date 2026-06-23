import React from 'react';
import { useTheme } from '../ThemeContext';
import HamburgerMenu from './HamburgerMenu';
import '../css/Music.css';
import '../css/Settings.css';

const Settings = () => {
  const { isDark, toggleDark } = useTheme();

  return (
    <div className="musicOuter settingsOuter">
      <div className="musicHeader">
        <div className="musicHeaderInner">
          <div className="musicHeaderRow">
            <HamburgerMenu />
            <p className="musicEyebrow">SETTINGS</p>
          </div>
          <h1 className="musicTitle">Settings</h1>
          <div className="musicHeaderRule" />
        </div>
      </div>

      <div className="musicPage settingsPage">
        <div className="settingsSection">
          <div className="settingsSectionTitle">APPEARANCE</div>
          <div className="settingsRow">
            <div className="settingsRowInfo">
              <div className="settingsRowLabel">Dark Mode</div>
              <div className="settingsRowDesc">Use a dark color scheme across all views</div>
            </div>
            <button
              className={`settingsToggle${isDark ? ' settingsToggle--on' : ''}`}
              onClick={toggleDark}
              role="switch"
              aria-checked={isDark}
              aria-label="Toggle dark mode"
            >
              <span className="settingsToggleThumb" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Settings;
