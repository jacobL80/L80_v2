import React from 'react';
import {
	BrowserRouter as Router,
	Route,
	Routes
  } from "react-router-dom";
import './App.css';

import Home from './components/Home';
import About from './components/About';
import Music from './components/Music';

const App = () => {
	return (
		<Router>
			<Routes>
				<Route exact path="/" element={<Home/>}/>
                <Route exact path="/about" element={<About/>}/>
                <Route exact path="/music" element={<Music/>}/>
			</Routes>
		</Router>
	);
}

export default App;
