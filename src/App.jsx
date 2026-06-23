import React from 'react';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import { ThemeProvider } from './ThemeContext';
import './App.css';

import Home     from './components/Home';
import About    from './components/About';
import Music    from './components/Music';
import Concerts from './components/Concerts';
import TvMovies from './components/TvMovies';
import Running  from './components/Running';
import AllView  from './components/AllView';
import Settings from './components/Settings';

const App = () => (
  <ThemeProvider>
    <Router>
      <Routes>
        <Route path="/"          element={<AllView />} />
        <Route path="/portfolio" element={<Home />} />
        <Route path="/about"     element={<About />} />
        <Route path="/music"     element={<Music />} />
        <Route path="/concerts"  element={<Concerts />} />
        <Route path="/tv"        element={<TvMovies />} />
        <Route path="/running"   element={<Running />} />
        <Route path="/settings"  element={<Settings />} />
      </Routes>
    </Router>
  </ThemeProvider>
);

export default App;
