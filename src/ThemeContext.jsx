import React, { createContext, useContext, useState, useEffect } from 'react';

const ThemeContext = createContext({ isDark: false, toggleDark: () => {} });

function readCookie() {
  const match = document.cookie.match(/(^|;\s*)theme=([^;]+)/);
  return match ? match[2] : null;
}

function writeCookie(value) {
  const expires = new Date(Date.now() + 365 * 864e5).toUTCString();
  document.cookie = `theme=${value}; expires=${expires}; path=/; SameSite=Lax`;
}

export const ThemeProvider = ({ children }) => {
  const [isDark, setIsDark] = useState(() => {
    const saved = readCookie();
    const dark = saved === 'dark';
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light');
    return dark;
  });

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', isDark ? 'dark' : 'light');
    writeCookie(isDark ? 'dark' : 'light');
  }, [isDark]);

  return (
    <ThemeContext.Provider value={{ isDark, toggleDark: () => setIsDark(v => !v) }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => useContext(ThemeContext);
