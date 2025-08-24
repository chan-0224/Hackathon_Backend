const express = require('express');
const router = express.Router();
const axios = require('axios');
const pool = require('../models/db'); // PostgreSQL 연결

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

// 2. 선택한 지역 설정 및 행사 가져오기
// POST로 선택한 지역을 보내면, 바로 해당 지역 기준 행사 반환
router.post('/events', async (req, res) => {
  const { district } = req.body;

  if (!district) {
    return res.status(400).json({ error: 'district 필요' });
  }

  try {
    // 실제 AI 백엔드 URL로 교체 필요
    const aiResponse = await axios.get('실제 ai백엔드 URL 연결', {
      params: { district }
    });

    res.json({
      message: `지역 "${district}" 기준 행사 조회`,
      events: aiResponse.data
    });
  } catch (err) {
    console.error('AI 호출 오류:', err);
    res.status(500).json({ error: 'AI 백엔드 호출 실패' });
  }
});

module.exports = router;