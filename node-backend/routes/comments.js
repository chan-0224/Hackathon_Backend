const express = require('express');
const router = express.Router();
const pool = require('../models/db');

// 댓글 작성
router.post('/', async (req, res) => {
  const { post_id, author_name, content } = req.body;
  if (!post_id || !content) return res.status(400).json({ error: 'post_id와 content 필수' });

  try {
    const result = await pool.query(
      'INSERT INTO comments(post_id, author_name, content) VALUES($1,$2,$3) RETURNING *',
      [post_id, author_name || '익명', content]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 댓글 목록 조회
router.get('/', async (req, res) => {
  const { post_id } = req.query;
  if (!post_id) return res.status(400).json({ error: 'post_id 필요' });

  try {
    const result = await pool.query(
      'SELECT * FROM comments WHERE post_id=$1 AND is_deleted = FALSE ORDER BY created_at ASC',
      [post_id]
    );
    res.json(result.rows);
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// 댓글 수정
router.put('/:id', async (req, res) => {
  const { id } = req.params;
  const { content } = req.body;
  if (!content) return res.status(400).json({ error: 'content 필요' });

  try {
    const result = await pool.query(
      'UPDATE comments SET content=$1, updated_at=NOW() WHERE id=$2 AND is_deleted = FALSE RETURNING *',
      [content, id]
    );
    if (!result.rows.length) return res.status(404).json({ error: '댓글 없음' });
    res.json(result.rows[0]);
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// 댓글 삭제 (소프트 삭제)
router.delete('/:id', async (req, res) => {
  const { id } = req.params;
  try {
    const result = await pool.query(
      'UPDATE comments SET is_deleted = TRUE WHERE id=$1 RETURNING *',
      [id]
    );
    if (!result.rows.length) return res.status(404).json({ error: '댓글 없음' });
    res.json({ message: '댓글 삭제됨' });
  } catch (err) { res.status(500).json({ error: err.message }); }
});

module.exports = router;