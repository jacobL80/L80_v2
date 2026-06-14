import React, { useState, useLayoutEffect, useRef, useEffect } from 'react';
import { Link } from "react-router-dom";
import '../css/Home.css';
import arrow from '../assets/arrow.png';
import Resume from '../Jacob_Leighty_Resume.pdf';
import pageContent from '../contents';

const screenshots = import.meta.glob('../assets/screenshots/*', { eager: true });
const documents = import.meta.glob('../assets/documents/*', { eager: true });
const screenshot = (name, ext) => screenshots[`../assets/screenshots/${name}.${ext}`]?.default;
const document_ = (name) => documents[`../assets/documents/${name}.pdf`]?.default;

const S = (props) => <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props} />;

const UserIcon     = () => <S><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></S>;
const GridIcon     = () => <S><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></S>;
const SmileIcon    = () => <S><circle cx="12" cy="12" r="10"/><path d="M8 13s1.5 2 4 2 4-2 4-2"/><line x1="9" y1="9" x2="9.01" y2="9"/><line x1="15" y1="9" x2="15.01" y2="9"/></S>;
const ResumeIcon   = () => <S><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></S>;
const MailIcon     = () => <S><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></S>;
const PhoneIcon    = () => <S><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.65 3.35 2 2 0 0 1 3.62 1h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9a16 16 0 0 0 6.29 6.29l.78-.78a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></S>;
const LinkedInIcon = () => <S><path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z"/><rect x="2" y="9" width="4" height="12"/><circle cx="4" cy="4" r="2"/></S>;

function Home() {
	useEffect(() => { document.title = "Jacob Leighty's UX/UI Portfolio"; }, []);
	const [hoveredWork, setHoveredWork] = useState(0);
	const [selectedWork, setSelectedWork] = useState(0);
	const [mobileView, setMobileView] = useState('info');
	const data = pageContent.pages[selectedWork - 1];

	const introRef = useRef(null);
	const worksRef = useRef(null);

	useLayoutEffect(() => {
		const sync = () => {
			if (!worksRef.current) return;
			if (!introRef.current || window.innerWidth <= 900) {
				worksRef.current.style.paddingTop = '';
				return;
			}
			const offset = introRef.current.getBoundingClientRect().top
				- worksRef.current.getBoundingClientRect().top;
			worksRef.current.style.paddingTop = Math.max(0, offset) + 'px';
		};
		sync();
		window.addEventListener('resize', sync);
		return () => window.removeEventListener('resize', sync);
	}, [selectedWork]);

	return (
	<div className={`app${mobileView === 'works' ? ' app--works' : ''}`}>

		<div className='appContainer'>
			<div className='leftPane'>
				<div className='innerPanel left'>
					{selectedWork === 0 ? (
						<div className='contentContainer'>
							<div className='contentBlock introBlock' ref={introRef}>
								<span className='textEyebrow'>Portfolio</span>
								<span className='text textTitle'>Hi there!</span>
								<div className='headerRule' />
								<span className='text'>{"My name is "}</span>
								<span className='text textHighlight'>Jacob Leighty.</span>
							</div>
							<div className='contentBlock infoBlock'>
								<span className='text'>
									{"I'm a "}
									<span className='text textBold'>UX/UI Designer</span>
									{" and "}
									<span className='text textBold'>Software Engineer</span>
									{" based in "}
									<span className='text textHighlight textHighlightBlue'>Seattle, WA.</span>
								</span>
								<span className='text'>
									{"Take a look at some of my works here. "}
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
					<div className='contentContainer contentRight' ref={worksRef}>
						<span className='textSubtitle'>Featured Works</span>
						<div className='headerRuleRight' />
						<div className='worksList'>
							{pageContent.pages.map((page, i) => {
								const workIndex = i + 1;
								const isActive = hoveredWork === workIndex || selectedWork === workIndex || (hoveredWork === 0 && selectedWork === 0);
								const isSelected = selectedWork === workIndex;
								return (
									<span
										key={workIndex}
										className={`textWorkLabel${isSelected ? ' textWorkLabel--selected' : ''}${!isActive ? ' textNotSelected' : ''}`}
										onMouseOver={() => setHoveredWork(workIndex)}
										onMouseOut={() => setHoveredWork(0)}
										onClick={() => { setSelectedWork(workIndex); setMobileView('info'); }}
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

		{/* Mobile-only bottom tab bar: Info / Works | contacts */}
		<div className="mobileTabBar">
			<button
				className={`mobileTabBtn${mobileView === 'info' ? ' mobileTabBtn--active' : ''}`}
				onClick={() => setMobileView('info')}
			>
				<UserIcon />
				<span>Info</span>
			</button>
			<button
				className={`mobileTabBtn${mobileView === 'works' ? ' mobileTabBtn--active' : ''}`}
				onClick={() => setMobileView('works')}
			>
				<GridIcon />
				<span>Works</span>
			</button>
			<Link to="/about" className="mobileTabContact">
				<SmileIcon /><span>About</span>
			</Link>
			<div className="mobileTabDivider" />
			<a href={Resume} target="_blank" rel="noreferrer" className="mobileTabContact">
				<ResumeIcon /><span>Resume</span>
			</a>
			<a href="mailto:jacob.leighty@gmail.com" className="mobileTabContact">
				<MailIcon /><span>Email</span>
			</a>
			<a href="tel:12538806289" className="mobileTabContact">
				<PhoneIcon /><span>Phone</span>
			</a>
			<a href="https://www.linkedin.com/in/jacob-leighty/" target="_blank" rel="noreferrer" className="mobileTabContact">
				<LinkedInIcon /><span>LinkedIn</span>
			</a>
		</div>
	</div>
	);
}

export default Home;
