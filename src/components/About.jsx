import React, { useState } from 'react';
import { Link } from "react-router-dom";
import logo_2 from '../assets/logo_2.png';
import menu_2 from '../assets/menu_2.png';
import '../css/About.css';
import selfie from '../assets/me2.jpg';
import ContactBar from './ContactBar';

function About() {
    const [menuHover, setMenuHover] = useState(true);

	return (
	<div className="app">
		<div className='appContainer'>
			<div className='leftPaneAbout'>
				<div className='innerPanel left'>
					<div className='menuContainer'>
                        <Link to="/"
							className='menuLinkAbout'
							onMouseEnter={() => setMenuHover(false)}
							onMouseLeave={() => setMenuHover(true)}>
							<img src={logo_2} className="logoAbout" alt="logo"/>
						</Link>
                        <span className={menuHover ? 'menuTextAbout' : 'menuTextAbout menuTextHoverAbout'}>Return Home</span>
					</div>
					<div className='contentContainer selfieContentContainer'>
						<img src={selfie} alt="Jacob and husband on the Oregon shore" className="selfie"/>
						<span className='selfieText'>{"Me (on the right) and my husband on the Oregon shore."}</span>
					</div>
				</div>
			</div>
			<div className='rightPaneAbout'>
				<div className='innerPanel right'>
					<div className='menuContainer'>
						<ContactBar />
						<img src={menu_2} className="menuAbout" alt="menu"/>
					</div>
					<div className='contentBlockAbout introBlock'>
                        <span className='text textGeneric'>{"My name is "}</span>
                        <span className='text textHighlight'>Jacob Leighty.</span>
                    </div>
					<div className='contentBlockAbout infoBlock'>
                        <span className='textAbout textGeneric'>
                            {"I'm a hybrid "}
                            <span className='textAbout textBold'>UX/UI Designer and Software Engineer.</span>
                            {" While I have a passion for expressive and intuitive designs, I also pride myself in my coding prowess; this is especially true with both "}
                            <span className='textAbout textBold'>React and React Native.</span>
                            {" I studied "}
                            <span className='textAbout textBold'>Information Science</span>
                        </span>
                        <span className='textAbout textGeneric'>
                            {" at the University of Washington. All designs shown, as well as the code running this portfolio, were written and created by me!"}
                        </span>
                    </div>
                    <div className='contentBlockAbout infoBlock'>
                        <span className='textAbout textGeneric'>In my free time, I enjoy collecting music, working out, and indoor bouldering.</span>
                    </div>
				</div>
			</div>
		</div>
	</div>
	);
}

export default About;
