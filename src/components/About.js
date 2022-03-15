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
							<img src={logo_2} className="logoAbout"/>
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
                            <a href="../Jacob_Leighty_Resume.pdf" target="_blank"><img src={resume_icon} className="contactIcon"/></a>
							<a href="mailto:jacob.leighty@gmail.com"><img src={email_icon} className="contactIcon"/></a>
							<a href="tel:12538806289"><img src={phone_icon} className="contactIcon"/></a>
							<a href="https://www.linkedin.com/in/jacob-leighty/" target="_blank"><img src={linkedin_icon} className="contactIcon"/></a>
						</div>
						<img src={menu_2} className="menuAbout"/>
					</div>
					<div className='contentBlock introBlock'>
                        <text className='text textGeneric'>{"My name is "}</text>
                        <text className='text textHighlight'>Jacob Leighty.</text>
                    </div>
					<div className='contentBlock infoBlock'>
                        <text className='textAbout textGeneric'>
                            {"I'm a hybrid "}
                            <text className='textAbout textBold'>UX/UI Designer and Software Developer.</text>
                            {" While I have a passion for expressive and intuitive designs, I also pride myself in my coding prowess; this is especially true with both "}
                            <text className='textAbout textBold'>React and React Native.</text>
                            {" I studied "}
                            <text className='textAbout textBold'>Information Science</text>
                        </text>
                        <text className='textAbout textGeneric'>
                            {" at the University of Washington. All designs shown, as well as the code running this portfolio, were written/created by me!"}
                        </text>
                    </div>

                    <div className='contentBlock infoBlock'>
                        <text className='textAbout textGeneric'>In my free time, I enjoy collecting music, working out, and competitive gaming.</text>
                    </div>
				</div>
			</div>
		</div>
	</div>
	);
}

export default About;