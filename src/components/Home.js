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
import pageContent from '../contents'
import Resume from "../Jacob_Leighty_Resume.pdf"

function Home() {
	const [menuHover, toggleMenuHover] = useState(true);
	const [hoveredIcon, setHoveredIcon] = useState("none");
	const [hoveredWork, setHoveredWork] = useState(0);
	const [selectedWork, setSelectedWork] = useState(0);
	const data = pageContent.pages[selectedWork - 1];

	return (
	<div className="app">
		<div className='appContainer'>
			<div className='leftPane'>
				<div className='innerPanel left'>
					<div className='menuContainer menuContainerList'>
						<img src={logo_1} alt="logo" className={selectedWork !==  0 ? "logo logoReturn" : "logo"} 
							onMouseOver={() => selectedWork !==  0 ? setHoveredIcon("Return Home") : null}
							onMouseOut={() => selectedWork !==  0 ? setHoveredIcon("none") : null}
							onClick={() => selectedWork !==  0 ? (setSelectedWork(0), setHoveredIcon("none")) : null}/>
						<div className='contactOuterContainer'>
							<div className="contactRow">
								<a href={Resume} target="_blank" rel="noreferrer">
									<img src={resume_icon} alt="resume" className={hoveredIcon !== "none" && hoveredIcon !== "resume" ? "contactIcon contactIconNotSelected" : "contactIcon"} 
										onMouseOver={() => setHoveredIcon("resume")} 
										onMouseOut={() => setHoveredIcon("none")} /></a>
								<a href="mailto:jacob.leighty@gmail.com">
									<img src={email_icon} alt="email" className={hoveredIcon !== "none" && hoveredIcon !== "email address" ? "contactIcon contactIconNotSelected" : "contactIcon"} 
										onMouseOver={() => setHoveredIcon("email address")} 
										onMouseOut={() => setHoveredIcon("none")}/></a>
								<a href="tel:12538806289">
									<img src={phone_icon} alt="phone number" className={hoveredIcon !== "none" && hoveredIcon !== "phone number" ? "contactIcon contactIconNotSelected" : "contactIcon"} 
										onMouseOver={() => setHoveredIcon("phone number")} 
										onMouseOut={() => setHoveredIcon("none")}/></a>
								<a href="https://www.linkedin.com/in/jacob-leighty/" target="_blank" rel="noreferrer" >
									<img src={linkedin_icon} alt="LinkedIn" className={hoveredIcon !== "none" && hoveredIcon !== "linkedin" ? "contactIcon contactIconNotSelected" : "contactIcon"} 
										onMouseOver={() => setHoveredIcon("linkedin")} 
										onMouseOut={() => setHoveredIcon("none")}/></a>
							</div>
							<div className='contactTitleContainer'><text className={hoveredIcon !== "none" ? 'contactTitle' : 'contactTitle contactTitleNone'}>{hoveredIcon}</text></div>
						</div>
					</div>
					{selectedWork === 0 ? <div className='contentContainer'>
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
					</div> : 
					<div className='contentContainer contentContainerSelected'>
						<img className='contentImage contentImageHeader' alt={"image" + data.projectNumber} src={require('../assets/screenshots/' + data.projectImage + ".png")}/>
						<div className='contentSection contentOverview'>
							<text className='contentText contentTextTitle'>{data.projectName}</text>
							<text className='contentText contentTextDate'>{data.date}</text>
							<text className='contentText contentTextLabel'>Role</text>
							<text className='contentText contentTextRole'>{data.role}</text>
							
							<text className='contentText contentTextOverview'>{data.overview}</text>
						</div>

						<div className='contentSection'>
							{
							Object.keys(data.sections).map((key, i) => (
								<div key={i}>
									<div className='contentSectionTitle'>{key}</div>
									{Object.keys(data.sections[key]).map((innerKey, j) => (
										<div className='contentSectionParagraph'>{innerKey.startsWith("image")
											? <img className='contentImage' alt={innerKey + j} src={require ('../assets/screenshots/' + data.sections[key][innerKey] + '.png')}/>
											: innerKey.startsWith("gif")
												? <img className='contentAnimation' alt={innerKey + j} src={require ('../assets/screenshots/' + data.sections[key][innerKey] + '.gif')}/>
												: innerKey.startsWith("link")
													? <a href={require("../assets/documents/" + data.sections[key][innerKey] + ".pdf")} target="_blank" rel='noopener noreferrer'><text className='contentLink'>{data.sections[key][innerKey] + ".pdf"}</text>
													</a>
													: data.sections[key][innerKey]
										}</div>
									))}
								</div>
							))}

							</div>
					</div>}
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
							<text className={(hoveredWork === 1) ||(selectedWork ===  1) || (hoveredWork === 0 && selectedWork === 0) ? "text textBold textWorkLabel" : "text textBold textWorkLabel textNotSelected"} 		
								onMouseOver={() => setHoveredWork(1)} 
								onMouseOut={() => setHoveredWork(0)}
								onClick={() => setSelectedWork(1)}>Quantum App Center</text>
							<text className={(hoveredWork === 2) || (selectedWork ===  2) || (hoveredWork === 0 && selectedWork === 0) ? "text textBold textWorkLabel" : "text textBold textWorkLabel textNotSelected"} 
								onMouseOver={() => setHoveredWork(2)} 
								onMouseOut={() => setHoveredWork(0)}
								onClick={() => setSelectedWork(2)}>Quantum Branding</text>
							<text className={(hoveredWork === 3) || (selectedWork ===  3) || (hoveredWork === 0 && selectedWork === 0) ? "text textBold textWorkLabel" : "text textBold textWorkLabel textNotSelected"}
								onMouseOver={() => setHoveredWork(3)} 
								onMouseOut={() => setHoveredWork(0)}
								onClick={() => setSelectedWork(3)}>Customer Insights</text>
							<text className={(hoveredWork === 4) || (selectedWork ===  4) || (hoveredWork === 0 && selectedWork === 0) ? "text textBold textWorkLabel" : "text textBold textWorkLabel textNotSelected"}
								onMouseOver={() => setHoveredWork(4)} 
								onMouseOut={() => setHoveredWork(0)}
								onClick={() => setSelectedWork(4)}>Q-Help</text>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	);
}

export default Home;
