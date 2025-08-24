const express = require('express');
const router = express.Router();
const LikeModel = require('../models/likeModel');

// 좋아요 추가
router.post('/', async (req, res) => {
  const { post_id, author_name } = req.body;
  if (!post_id || !author_name)
    return res.status(400).json({ error: 'post_id와 author_name 필요' });

  try {
    const like = await LikeModel.addLike(post_id, author_name);
    if (!like) return res.json({ message: '이미 좋아요함' });
    res.json(like);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 좋아요 삭제
router.delete('/', async (req, res) => {
  const { post_id, author_name } = req.body;
  if (!post_id || !author_name)
    return res.status(400).json({ error: 'post_id와 author_name 필요' });

  try {
    const like = await LikeModel.removeLike(post_id, author_name);
    if (!like) return res.status(404).json({ error: '좋아요 없음' });
    res.json({ message: '좋아요 취소됨' });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 특정 글 좋아요 수 조회
router.get('/', async (req, res) => {
  const { post_id } = req.query;
  if (!post_id) return res.status(400).json({ error: 'post_id 필요' });

  try {
    const likes = await LikeModel.countLikes(post_id);
    res.json({ likes });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 특정 사용자가 글을 좋아요했는지 확인
router.get('/check', async (req, res) => {
  const { post_id, author_name } = req.query;
  if (!post_id || !author_name)
    return res.status(400).json({ error: 'post_id와 author_name 필요' });

  try {
    const liked = await LikeModel.isLikedByUser(post_id, author_name);
    res.json({ liked });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;