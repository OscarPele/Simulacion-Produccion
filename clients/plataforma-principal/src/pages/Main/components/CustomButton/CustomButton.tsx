import { useState } from 'react';
import './CustomButton.scss';
import customButton from '../../assets/boton.png';

export default function CustomButton() {
  const [contador, setContador] = useState(0);

  const manejarClick = () => {
    setContador(contador + 1);
  };

  return (
    <div className="contenedor-de-prueba">
      <div className="boton-con-texto" onClick={manejarClick}>
        <img 
          src={customButton} 
          alt="custom-button" 
          className="custom-button" 
        />
        <span className="texto-en-boton">Click</span>
      </div>

      <p>Contador: {contador}</p>
    </div>
  );
}
