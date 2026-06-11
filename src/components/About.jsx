import React from 'react';
import { Link } from "react-router-dom";
import '../css/About.css';
import selfie from '../assets/me2.jpg';
import Resume from '../Jacob_Leighty_Resume.pdf';

const S = (props) => <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props} />;

const L80Icon = () => (
	<svg width="36" height="20" viewBox="0 0 36 20" fill="currentColor" stroke="none">
		<text x="18" y="15" textAnchor="middle" fontFamily="Calibri, sans-serif" fontWeight="700" fontSize="15" letterSpacing="1">L80</text>
	</svg>
);
const ResumeIcon   = () => <S><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></S>;
const MailIcon     = () => <S><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></S>;
const PhoneIcon    = () => <S><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07A19.5 19.5 0 0 1 4.69 12 19.79 19.79 0 0 1 1.65 3.35 2 2 0 0 1 3.62 1h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9a16 16 0 0 0 6.29 6.29l.78-.78a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/></S>;
const LinkedInIcon = () => <S><path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z"/><rect x="2" y="9" width="4" height="12"/><circle cx="4" cy="4" r="2"/></S>;

function About() {
	return (
	<div className="app">

		<div className='appContainer'>
			<div className='leftPaneAbout'>
				<div className='innerPanel left'>
					<div className='contentContainer selfieContentContainer'>
						<img src={selfie} alt="Jacob and husband on the Oregon shore" className="selfie"/>
						<span className='selfieText'>{"Me (on the right) and my husband on the Oregon shore."}</span>
					</div>
				</div>
			</div>
			<div className='rightPaneAbout'>
				<div className='innerPanel right'>
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

		{/* Mobile-only bottom nav: Home | contacts */}
		<div className="aboutTabBar">
			<Link to="/" className="aboutTabContact">
				<L80Icon /><span>Home</span>
			</Link>
			<div className="aboutTabDivider" />
			<a href={Resume} target="_blank" rel="noreferrer" className="aboutTabContact">
				<ResumeIcon /><span>Resume</span>
			</a>
			<a href="mailto:jacob.leighty@gmail.com" className="aboutTabContact">
				<MailIcon /><span>Email</span>
			</a>
			<a href="tel:12538806289" className="aboutTabContact">
				<PhoneIcon /><span>Phone</span>
			</a>
			<a href="https://www.linkedin.com/in/jacob-leighty/" target="_blank" rel="noreferrer" className="aboutTabContact">
				<LinkedInIcon /><span>LinkedIn</span>
			</a>
		</div>
	</div>
	);
}

export default About;
