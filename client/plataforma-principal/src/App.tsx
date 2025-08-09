
import './App.css';
import RegisterForm from './components/Register/RegisterForm';
import { FaCog } from "react-icons/fa";

function App() {
  return (
    <div>
      <div className='company-logo'>
      <div className='company-header'>
        <h1 className='company-acronym'>O.P.S.</h1>
        <FaCog className='company-gear' />
      </div>
      <p className='company-complete-name'>Oscar Production Simulator</p>
    </div>
      
      <div className='forms'>
        {/* Login Form */ }
        <RegisterForm/>
      </div>
      
    </div>
  );
}

export default App;
