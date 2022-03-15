import React, { useState } from 'react';
import {
	BrowserRouter as Router,
	Route,
	Routes,
	Link
  } from "react-router-dom";
import logo_1 from '../assets/logo_1.png'
import logo_2 from '../assets/logo_2.png'
import menu_1 from '../assets/menu_1.png'
import menu_2 from '../assets/menu_2.png'
import resume_icon from '../assets/resume_icon.png'
import email_icon from '../assets/email_icon.png'
import phone_icon from '../assets/phone_icon.png'
import linkedin_icon from '../assets/linkedin_icon.png'
import arrow from '../assets/arrow.png'
import '../css/About.css'

function About() {
    const [menuHover, toggleMenuHover] = useState(true);

	return (
	<div className="app">
		<div className='appContainer'>
			<div className='leftPaneAbout'>
				<div className='innerPanel left'>
					<div className='menuContainer'>
						
                        <Link to="/" 
							className='menuLinkAbout' 
							onMouseEnter={() => toggleMenuHover(false)} 
							onMouseLeave={() => toggleMenuHover(true)}>
							<img src={logo_2} className="logo"/>
						</Link>
                        <text className={menuHover ? 'menuTextAbout' : 'menuTextAbout menuTextHoverAbout'}>Return Home</text>
					</div>
					<div className='contentContainer'>
						
					</div>
				</div>
			</div>
			<div className='rightPaneAbout'>
				<div className='innerPanel right'>
					<div className='menuContainer'>
                        <div className="contactRow">
							<img src={resume_icon} className="contactIcon"/>
							<img src={email_icon} className="contactIcon"/>
							<img src={phone_icon} className="contactIcon"/>
							<img src={linkedin_icon} className="contactIcon"/>
						</div>
						<img src={menu_2} className="menu"/>
					</div>
					
					
				</div>
			</div>
		</div>
	</div>
	);
}

export default About;