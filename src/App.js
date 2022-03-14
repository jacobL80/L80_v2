import './App.css';
import logo_1 from './assets/logo_1.png'
import logo_2 from './assets/logo_2.png'
import menu_1 from './assets/menu_1.png'
import menu_2 from './assets/menu_2.png'
import resume_icon from './assets/resume_icon.png'
import email_icon from './assets/email_icon.png'
import phone_icon from './assets/phone_icon.png'
import linkedin_icon from './assets/linkedin_icon.png'
import arrow from './assets/arrow.png'

function App() {
	return (
	<div className="App">
		<div className='leftPane'>
			<div className='innerPanel'>
				<div className='menuContainer'>
					<img src={logo_1} className="logo"/>
				</div>
				<div className='contentContainer'>
					
					<div className='contentBlock introBlock'>
						<text className='text textTitle'>Hi there!</text>
						<text className='text textGeneric'>{"My name is "}
							<text className='text textHighlight'>Jacob Leighty.</text>
						</text>
						
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
							{"Take a look at my works here. "}
							<img src={arrow} className="arrowIcon"/>
						</text>
					</div>
					<div className="contactRow">
						<img src={resume_icon} className="contactIcon"/>
						<img src={email_icon} className="contactIcon"/>
						<img src={phone_icon} className="contactIcon"/>
						<img src={linkedin_icon} className="contactIcon"/>
					</div>
				</div>
			</div>
		</div>
		<div className='rightPane'>
			<div className='innerPanel'>
				<img src={menu_1} className="menu"/>
				<div className='contentContainer contentRight'>
					<text className='text textSubtitle'>Featured Works</text>
					<div className='worksList'>
						<text className='text textBold'>Quantum App Center</text>
						<text className='text textBold'>Quantum Branding</text>
						<text className='text textBold'>Q-Help</text>
						<text className='text textBold'>T-Mobile Internal Mobile Apps</text>
						<text className='text textBold'>Graphic Design at T-Mobile</text>
					</div>
					<text className='text textSubtitle'>See More</text>
				</div>
			</div>
		</div>
	</div>
	);
}

export default App;
