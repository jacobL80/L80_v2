import React, { useState } from 'react';
import { Link } from "react-router-dom";
import '../css/Home.css';
import logo_1 from '../assets/logo_1.png';
import menu_1 from '../assets/menu_1.png';
import arrow from '../assets/arrow.png';
import pageContent from '../contents';
import ContactBar from './ContactBar';

const screenshots = import.meta.glob('../assets/screenshots/*', { eager: true });
const documents = import.meta.glob('../assets/documents/*', { eager: true });
const screenshot = (name, ext) => screenshots[`../assets/screenshots/${name}.${ext}`]?.default;
const document_ = (name) => documents[`../assets/documents/${name}.pdf`]?.default;

function Home() {
	const [menuHover, setMenuHover] = useState(true);
	const [hoveredWork, setHoveredWork] = useState(0);
	const [selectedWork, setSelectedWork] = useState(0);
	const [logoHovered, setLogoHovered] = useState(false);
	const data = pageContent.pages[selectedWork - 1];

	return (
	<div className="app">
		<div className='appContainer'>
			<div className='leftPane'>
				<div className='innerPanel left'>
					<div className='menuContainer menuContainerList'>
						<img src={logo_1} alt="logo" className={selectedWork !== 0 ? 'logo logoReturn' : 'logo'}
							onMouseOver={() => { if (selectedWork !== 0) setLogoHovered(true); }}
							onMouseOut={() => setLogoHovered(false)}
							onClick={() => { if (selectedWork !== 0) setSelectedWork(0); }}/>
						<ContactBar label={logoHovered && selectedWork !== 0 ? 'Return Home' : null} />
					</div>
					{selectedWork === 0 ? (
						<div className='contentContainer'>
							<div className='contentBlock introBlock'>
								<span className='text textTitle'>Hi there!</span>
								<span className='text textGeneric'>{"My name is "}</span>
								<span className='text textHighlight'>Jacob Leighty.</span>
							</div>
							<div className='contentBlock infoBlock'>
								<span className='text textGeneric'>
									{"I'm a "}
									<span className='text textBold'>UX/UI Designer</span>
									{" and "}
									<span className='text textBold'>Software Engineer</span>
									{" based in "}
									<span className='text textHighlight textHighlightBlue'>Seattle, WA.</span>
								</span>
								<span className='text textGeneric'>
									{" Take a look at some of my works here. "}
									<img src={arrow} alt="look over there" className="arrowIcon"/>
								</span>
							</div>
						</div>
					) : (
						<div className='contentContainer contentContainerSelected'>
							<img className='contentImage contentImageHeader' alt={"image" + data.projectNumber} src={screenshot(data.projectImage, 'png')}/>
							<div className='contentSection contentOverview'>
								<span className='contentText contentTextTitle'>{data.projectName}</span>
								<span className='contentText contentTextDate'>{data.date}</span>
								<span className='contentText contentTextLabel'>Role</span>
								<span className='contentText contentTextRole'>{data.role}</span>
								<span className='contentText contentTextOverview'>{data.overview}</span>
							</div>
							<div className='contentSection'>
								{Object.keys(data.sections).map((key, i) => (
									<div key={i}>
										<div className='contentSectionTitle'>{key}</div>
										{Object.keys(data.sections[key]).map((innerKey, j) => (
											<div className='contentSectionParagraph' key={j}>
												{innerKey.startsWith("image")
													? <img className='contentImage' alt={innerKey + j} src={screenshot(data.sections[key][innerKey], 'png')}/>
													: innerKey.startsWith("gif")
														? <img className='contentAnimation' alt={innerKey + j} src={screenshot(data.sections[key][innerKey], 'gif')}/>
														: innerKey.startsWith("link")
															? <a href={document_(data.sections[key][innerKey])} target="_blank" rel='noopener noreferrer'>
																<span className='contentLink'>{data.sections[key][innerKey] + ".pdf"}</span>
															</a>
															: data.sections[key][innerKey]
												}
											</div>
										))}
									</div>
								))}
							</div>
						</div>
					)}
				</div>
			</div>
			<div className='rightPane'>
				<div className='innerPanel right'>
					<div className='menuContainer'>
						<span className={menuHover ? 'menuText' : 'menuText menuTextHover'}>Go to 'About Me'</span>
						<Link to="/about"
							className='menuLink'
							onMouseEnter={() => setMenuHover(false)}
							onMouseLeave={() => setMenuHover(true)}>
							<img src={menu_1} alt="menu" className="menu"/>
						</Link>
					</div>
					<div className='contentContainer contentRight'>
						<span className='text textSubtitle'>Featured Works</span>
						<div className='worksList'>
							{pageContent.pages.map((page, i) => {
								const workIndex = i + 1;
								const isActive = hoveredWork === workIndex || selectedWork === workIndex || (hoveredWork === 0 && selectedWork === 0);
								return (
									<span
										key={workIndex}
										className={`text textBold textWorkLabel${isActive ? '' : ' textNotSelected'}`}
										onMouseOver={() => setHoveredWork(workIndex)}
										onMouseOut={() => setHoveredWork(0)}
										onClick={() => setSelectedWork(workIndex)}
									>
										{page.projectName}
									</span>
								);
							})}
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
	);
}

export default Home;
