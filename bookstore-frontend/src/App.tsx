import { useState } from 'react';
import axios from 'axios';

export default function App() {
  const [keyword, setKeyword] = useState('');

  const handleSend = async () => {
    try {
      const response = await axios.post('https://localhost/api/books/search', {
        title: keyword
      });

      alert(`백엔드 응답 : ${JSON.stringify(response.data)}`);
    } catch (error) {
      console.error('전송 실패 :', error);
      alert(`전송 실패 : ${error}`);
    }
  }

  return (
    <div style={{ padding: '20px' }}>
      <h2>도서 검색 테스트</h2>
      <input
        type="text" value={keyword} onChange={(e) => setKeyword(e.target.value)} />
      <button onClick={handleSend}>전송</button>
    </div>
  )
}