import React from 'react';
import {
	BrowserRouter as Router,
	Route,
	Routes
  } from "react-router-dom";
import './App.css';

import Home from './components/Home.js';
import About from './components/About.js'

import logo_1 from './assets/logo_1.png'
import logo_2 from './assets/logo_2.png'
import menu_1 from './assets/menu_1.png'
import menu_2 from './assets/menu_2.png'
import resume_icon from './assets/resume_icon.png'
import email_icon from './assets/email_icon.png'
import phone_icon from './assets/phone_icon.png'
import linkedin_icon from './assets/linkedin_icon.png'
import arrow from './assets/arrow.png'

const App = () => {
	return (
		<Router>
			<Routes>
				<Route exact path="/" element={<Home/>}/>
                <Route exact path="/about" element={<About/>}/>
			</Routes>
		</Router>
	);
}

export default App;
