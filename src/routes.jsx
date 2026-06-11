import React from 'react';
import {
	BrowserRouter as Router,
	Routes,
	Route,
	Link
  } from "react-router-dom";
import App from './App';
import About from './components/About';

function SiteRoutes(){
    return (
        <Router>
            <Routes>
                <Route exact path="/" element={<App/>}/>
                <Route exact path="/" element={<About/>}/>
            </Routes>
        </Router>
    )
}

export default SiteRoutes;
