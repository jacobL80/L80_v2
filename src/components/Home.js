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
	const [hoveredIcon, setHoveredIcon] = useState("none");
	const [hoveredWork, setHoveredWork] = useState("");

	return (
	<div className="app">
		<div className='appContainer'>
			<div className='leftPane'>
				<div className='innerPanel left'>
					<div className='menuContainer'>
						<img src={logo_1} alt="logo" className="logo"/>
						<div className='contactOuterContainer'>
							<div className="contactRow">
								<a href="../Jacob_Leighty_Resume.pdf" target="_blank">
									<img src={resume_icon} alt="resume" className={hoveredIcon !== "none" && hoveredIcon !== "resume" ? "contactIcon contactIconNotSelected" : "contactIcon"} onMouseOver={() => setHoveredIcon("resume")} onMouseOut={() => setHoveredIcon("none")}/></a>
								<a href="mailto:jacob.leighty@gmail.com">
									<img src={email_icon} alt="email" className={hoveredIcon !== "none" && hoveredIcon !== "email address" ? "contactIcon contactIconNotSelected" : "contactIcon"} onMouseOver={() => setHoveredIcon("email address")} onMouseOut={() => setHoveredIcon("none")}/></a>
								<a href="tel:12538806289">
									<img src={phone_icon} alt="phone number" className={hoveredIcon !== "none" && hoveredIcon !== "phone number" ? "contactIcon contactIconNotSelected" : "contactIcon"} onMouseOver={() => setHoveredIcon("phone number")} onMouseOut={() => setHoveredIcon("none")}/></a>
								<a href="https://www.linkedin.com/in/jacob-leighty/" target="_blank" rel="noreferrer" >
									<img src={linkedin_icon} alt="LinkedIn" className={hoveredIcon !== "none" && hoveredIcon !== "linkedin" ? "contactIcon contactIconNotSelected" : "contactIcon"} onMouseOver={() => setHoveredIcon("linkedin")} onMouseOut={() => setHoveredIcon("none")}/></a>
							</div>
							<div className='contactTitleContainer'><text className={hoveredIcon !== "none" ? 'contactTitle' : 'contactTitle contactTitleNone'}>{hoveredIcon}</text></div>
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
								<text className='text textBold'>Software Engineer</text>
								{" based in "}
								<text className='text textHighlight textHighlightBlue'>Seattle, WA.</text>
							</text>
							<text className='text textGeneric'>
								{" Take a look at some of my works here. "}
								<img src={arrow} alt="look over there" className="arrowIcon"/>
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
							<img src={menu_1} alt="menu" className="menu"/>
						</Link>
					</div>
					
					<div className='contentContainer contentRight'>
						<text className='text textSubtitle'>Featured Works</text>
						<div className='worksList'>
							<text className={hoveredWork !== "" && hoveredWork !== "app-center" ? "text textBold textWorkLabel textNotSelected" : "text textBold textWorkLabel"} onMouseOver={() => setHoveredWork("app-center")} onMouseOut={() => setHoveredWork("")}>Quantum App Center</text>
							<text className={hoveredWork !== "" && hoveredWork !== "branding" ? "text textBold textWorkLabel textNotSelected" : "text textBold textWorkLabel"} onMouseOver={() => setHoveredWork("branding")} onMouseOut={() => setHoveredWork("")}>Quantum Branding</text>
							<text className={hoveredWork !== "" && hoveredWork !== "insights-web" ? "text textBold textWorkLabel textNotSelected" : "text textBold textWorkLabel"} onMouseOver={() => setHoveredWork("insights-web")} onMouseOut={() => setHoveredWork("")}>Customer Insights (Web)</text>
							<text className={hoveredWork !== "" && hoveredWork !== "insights-mobile" ? "text textBold textWorkLabel textNotSelected" : "text textBold textWorkLabel"} onMouseOver={() => setHoveredWork("insights-mobile")} onMouseOut={() => setHoveredWork("")}>Customer Insights (Mobile)</text>
							<text className={hoveredWork !== "" && hoveredWork !== "q-help" ? "text textBold textWorkLabel textNotSelected" : "text textBold textWorkLabel"} onMouseOver={() => setHoveredWork("q-help")} onMouseOut={() => setHoveredWork("")}>Q-Help</text>
						</div>
						{/*<text className='text textSubtitle textSubtitleBottom'>See More</text>*/}
					</div>
				</div>
			</div>
		</div>
	</div>
	);
}

export default Home;
