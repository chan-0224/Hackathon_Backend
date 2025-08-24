const express = require('express');
const router = express.Router();
const noticeModel = require('../models/noticeModel');

// 전체 공지사항 조회
router.get('/', async (req, res) => {
  try {
    const notices = await noticeModel.getAllNotices();
    res.json(notices);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 공지사항 추가
router.post('/', async (req, res) => {
  const { title, content } = req.body;
  if (!title || !content) return res.status(400).json({ error: "제목과 내용 필요" });

  try {
    const result = await noticeModel.createNotice(title, content);
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;