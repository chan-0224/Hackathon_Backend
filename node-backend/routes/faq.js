const express = require('express');
const router = express.Router();
const faqModel = require('../models/faqModel');

// 전체 FAQ 조회
router.get('/', async (req, res) => {
  try {
    const faqs = await faqModel.getAllFaqs();
    res.json(faqs);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// FAQ 추가 (관리자용)
router.post('/', async (req, res) => {
  const { question, answer } = req.body;
  if (!question || !answer) return res.status(400).json({ error: "질문과 답변 필요" });

  try {
    const result = await faqModel.createFaq(question, answer);
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;