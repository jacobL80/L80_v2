import React, { useState } from 'react';
import resume_icon from '../assets/resume_icon.png';
import email_icon from '../assets/email_icon.png';
import phone_icon from '../assets/phone_icon.png';
import linkedin_icon from '../assets/linkedin_icon.png';
import Resume from '../Jacob_Leighty_Resume.pdf';

function ContactBar({ label = null }) {
	const [hoveredIcon, setHoveredIcon] = useState('none');
	const displayLabel = label ?? hoveredIcon;

	const iconClass = (name) =>
		hoveredIcon !== 'none' && hoveredIcon !== name
			? 'contactIcon contactIconNotSelected'
			: 'contactIcon';

	return (
		<div className='contactOuterContainer'>
			<div className="contactRow">
				<a href={Resume} target="_blank" rel="noreferrer">
					<img src={resume_icon} alt="resume" className={iconClass('resume')}
						onMouseOver={() => setHoveredIcon('resume')}
						onMouseOut={() => setHoveredIcon('none')} />
				</a>
				<a href="mailto:jacob.leighty@gmail.com">
					<img src={email_icon} alt="email" className={iconClass('email address')}
						onMouseOver={() => setHoveredIcon('email address')}
						onMouseOut={() => setHoveredIcon('none')} />
				</a>
				<a href="tel:12538806289">
					<img src={phone_icon} alt="phone number" className={iconClass('phone number')}
						onMouseOver={() => setHoveredIcon('phone number')}
						onMouseOut={() => setHoveredIcon('none')} />
				</a>
				<a href="https://www.linkedin.com/in/jacob-leighty/" target="_blank" rel="noreferrer">
					<img src={linkedin_icon} alt="LinkedIn" className={iconClass('linkedin')}
						onMouseOver={() => setHoveredIcon('linkedin')}
						onMouseOut={() => setHoveredIcon('none')} />
				</a>
			</div>
			<div className='contactTitleContainer'>
				<span className={displayLabel !== 'none' ? 'contactTitle' : 'contactTitle contactTitleNone'}>
					{displayLabel}
				</span>
			</div>
		</div>
	);
}

export default ContactBar;
