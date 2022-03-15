import React, { useState } from 'react';
import {
	BrowserRouter as Router,
	Route,
	Routes,
	Link
  } from "react-router-dom";
import '../css/Home.css';
import logo_1 from '../assets/logo_1.png'
import menu_1 from '../assets/menu_1.png'
import resume_icon from '../assets/resume_icon.png'
import email_icon from '../assets/email_icon.png'
import phone_icon from '../assets/phone_icon.png'
import linkedin_icon from '../assets/linkedin_icon.png'
import arrow from '../assets/arrow.png'

function Home() {
	const [menuHover, toggleMenuHover] = useState(true);

	return (
	<div className="app">
		<div className='appContainer'>
			<div className='leftPane'>
				<div className='innerPanel left'>
					<div className='menuContainer'>
						<img src={logo_1} className="logo"/>
						<div className="contactRow">
							<a href="../Jacob_Leighty_Resume.pdf" target="_blank"><img src={resume_icon} className="contactIcon"/></a>
							<a href="mailto:jacob.leighty@gmail.com"><img src={email_icon} className="contactIcon"/></a>
							<a href="tel:12538806289"><img src={phone_icon} className="contactIcon"/></a>
							<a href="https://www.linkedin.com/in/jacob-leighty/" target="_blank"><img src={linkedin_icon} className="contactIcon"/></a>
						</div>
					</div>
					<div className='contentContainer'>
						<div className='contentBlock introBlock'>
							<text className='text textTitle'>Hi there!</text>
							<text className='text textGeneric'>{"My name is "}</text>
							<text className='text textHighlight'>Jacob Leighty.</text>
						</div>
						<div className='contentBlock infoBlock'>
							<text className='text textGeneric'>
								{"I'm a "}
								<text className='text textBold'>UX/UI Designer</text>
								{" and "}
								<text className='text textBold'>Software Developer</text>
								{" based in "}
								<text className='text textHighlight textHighlightBlue'>Seattle, WA.</text>
							</text>
							<text className='text textGeneric'>
								{" Take a look at some of my works here. "}
								<img src={arrow} className="arrowIcon"/>
							</text>
						</div>
						
					</div>
				</div>
			</div>
			<div className='rightPane'>
				<div className='innerPanel right'>
					<div className='menuContainer'>
						<text className={menuHover ? 'menuText' : 'menuText menuTextHover'}>Go to 'About Me'</text>
						<Link to="/about" 
							className='menuLink' 
							onMouseEnter={() => toggleMenuHover(false)} 
							onMouseLeave={() => toggleMenuHover(true)}>
							<img src={menu_1} className="menu"/>
						</Link>
					</div>
					
					<div className='contentContainer contentRight'>
						<text className='text textSubtitle'>Featured Works</text>
						<div className='worksList'>
							<text className='text textBold'>Quantum App Center</text>
							<text className='text textBold'>Quantum Branding</text>
							<text className='text textBold'>Q-Help</text>
							<text className='text textBold'>T-Mobile Internal Mobile Apps</text>
							<text className='text textBold'>Graphic Design at T-Mobile</text>
						</div>
						<text className='text textSubtitle textSubtitleBottom'>See More</text>
					</div>
				</div>
			</div>
		</div>
	</div>
	);
}

export default Home;
