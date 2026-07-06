import React from 'react';
import './Calculator.css';

function Calculator() {
  return (
    <div className="calculator-container">
      <div className="calculator">
        {/* 태양광 패널 */}
        <div className="calculator-header">
          <div className="solar-panel"></div>
        </div>

        {/* 디스플레이 화면 */}
        <div className="calculator-display-container">
          <input className="calculator-display" type="text" value="12345" readOnly />
        </div>

        {/* 계산기 버튼 그리드 */}
        <div className="calculator-buttons">
          <button type="button" className="btn btn-gray">7</button>
          <button type="button" className="btn btn-gray">8</button>
          <button type="button" className="btn btn-gray">9</button>
          <button type="button" className="btn btn-orange">x</button>
          
          <button type="button" className="btn btn-gray">4</button>
          <button type="button" className="btn btn-gray">5</button>
          <button type="button" className="btn btn-gray">6</button>
          <button type="button" className="btn btn-orange">+</button>

          <button type="button" className="btn btn-gray">1</button>
          <button type="button" className="btn btn-gray">2</button>
          <button type="button" className="btn btn-gray">3</button>
          <button type="button" className="btn btn-orange">-</button>

          <button type="button" className="btn btn-gray">0</button>
          <button type="button" className="btn btn-gray">.</button>
          <button type="button" className="btn btn-gray">/</button>
          <button type="button" className="btn btn-orange">=</button>
        </div>
      </div>
    </div>
  );
}

export default Calculator;
