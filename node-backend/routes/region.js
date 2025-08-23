const express = require('express');
const router = express.Router();
const axios = require('axios');
const pool = require('../models/db'); 


// 1. 지역 목록 조회
router.get('/list', async (req, res) => {
  try {
    const result = await pool.query('SELECT * FROM regions ORDER BY district');
    res.json({ regions: result.rows });
  } catch (err) {
    console.error('DB 조회 오류:', err);
    res.status(500).json({ error: '서버 오류' });
  }
});

// 2. 선택한 지역 기반으로 AI 백엔드에서 행사 가져오기
router.get('/events', async (req, res) => {
  const { district } = req.query;

  if (!district) {
    return res.status(400).json({ error: 'district 쿼리 필요' });
  }

  try {
    // 실제 AI 백엔드 URL로 교체 필요
    const aiResponse = await axios.get('실제 ai백엔드 URL 연결', {
      params: { district }
    });

    res.json({ events: aiResponse.data });
  } catch (err) {
    console.error('AI 호출 오류:', err);
    res.status(500).json({ error: 'AI 백엔드 호출 실패' });
  }
});

module.exports = router;