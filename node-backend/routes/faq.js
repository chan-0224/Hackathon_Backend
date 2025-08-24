const express = require('express');
const router = express.Router();
const faqModel = require('../models/faqModel');

router.get('/', async (req, res) => {
  try {
    const faqs = await faqModel.getAllFaqs();
    res.json(faqs); // category 포함해서 반환됨
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// FAQ 추가 (관리자용)
router.post('/', async (req, res) => {
  const { question, answer, category } = req.body;
  if (!question || !answer || !category)
    return res.status(400).json({ error: "질문, 답변, 카테고리 필요" });

  try {
    const result = await faqModel.createFaq(question, answer, category);
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;