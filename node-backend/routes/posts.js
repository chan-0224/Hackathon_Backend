const express = require('express');
const router = express.Router();
const pool = require('../models/db');
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const uploadDir = 'uploads/';
if (!fs.existsSync(uploadDir)) fs.mkdirSync(uploadDir);

const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadDir),
  filename: (req, file, cb) => cb(null, Date.now() + path.extname(file.originalname))
});
const upload = multer({ storage });

const validCategories = [
  'free',
  'promotion_personal',
  'promotion_official',
  'news',
  'hot'
];

function isAdmin(req) {
  return req.headers['x-admin'] === 'true';
}

// 글 작성
router.post('/', upload.single('image'), async (req, res) => {
  const { author_name, title, content, category, ai_generated } = req.body;
  const postCategory = validCategories.includes(category) ? category : 'free';

  if (postCategory === 'hot') return res.status(400).json({ error: 'HOT게시판 글은 작성할 수 없습니다.' });
  if (postCategory === 'promotion_official' && !(isAdmin(req) || ai_generated === 'true')) {
    return res.status(403).json({ error: '공식 지역 홍보 게시판 글 작성은 관리자/AI만 가능합니다.' });
  }
  if (!title || !content) return res.status(400).json({ error: '제목과 내용을 입력해주세요.' });

  const image_url = req.file ? `/uploads/${req.file.filename}` : null;

  try {
    const result = await pool.query(
      'INSERT INTO posts(author_name, title, content, category, image_url) VALUES($1,$2,$3,$4,$5) RETURNING *',
      [author_name || '익명', title, content, postCategory, image_url]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 글 목록 조회
router.get('/', async (req, res) => {
  let { category, page = 1, limit = 10, search, sort = 'desc' } = req.query;
  page = parseInt(page); limit = parseInt(limit);
  const offset = (page - 1) * limit;

  try {
    let baseQuery = 'SELECT * FROM posts WHERE is_deleted = FALSE';
    let params = [];
    const conditions = [];

    if (category && validCategories.includes(category)) { 
      conditions.push(`category = $${params.length + 1}`);
      params.push(category);
    }
    if (search) {
      conditions.push(`title ILIKE $${params.length + 1}`);
      params.push(`%${search}%`);
    }
    if (conditions.length) baseQuery += ' AND ' + conditions.join(' AND ');

    baseQuery += ` ORDER BY created_at ${sort.toLowerCase() === 'asc' ? 'ASC' : 'DESC'} LIMIT $${params.length + 1} OFFSET $${params.length + 2}`;
    params.push(limit, offset);

    const result = await pool.query(baseQuery, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 특정 글 조회
router.get('/:id', async (req, res) => {
  const { id } = req.params;
  try {
    await pool.query('UPDATE posts SET views = views + 1 WHERE id = $1 AND is_deleted = FALSE', [id]);
    const result = await pool.query('SELECT * FROM posts WHERE id = $1 AND is_deleted = FALSE', [id]);
    if (!result.rows.length) return res.status(404).json({ error: '게시글을 찾을 수 없습니다.' });
    res.json(result.rows[0]);
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// 글 수정
router.put('/:id', upload.single('image'), async (req, res) => {
  const { id } = req.params;
  const { title, content, category, ai_generated } = req.body;
  const postCategory = validCategories.includes(category) ? category : 'free';

  if (postCategory === 'hot') return res.status(400).json({ error: 'HOT게시판 글은 수정할 수 없습니다.' });
  if (postCategory === 'promotion_official' && !(isAdmin(req) || ai_generated === 'true')) {
    return res.status(403).json({ error: '공식 지역 홍보 게시판 글 수정은 관리자/AI만 가능합니다.' });
  }
  if (!title || !content) return res.status(400).json({ error: '제목과 내용을 입력해주세요.' });

  const image_url = req.file ? `/uploads/${req.file.filename}` : null;

  try {
    const result = image_url ?
      await pool.query('UPDATE posts SET title=$1, content=$2, category=$3, image_url=$4, updated_at=NOW() WHERE id=$5 AND is_deleted = FALSE RETURNING *',
        [title, content, postCategory, image_url, id]) :
      await pool.query('UPDATE posts SET title=$1, content=$2, category=$3, updated_at=NOW() WHERE id=$4 AND is_deleted = FALSE RETURNING *',
        [title, content, postCategory, id]);

    if (!result.rows.length) return res.status(404).json({ error: '글을 찾을 수 없습니다.' });
    res.json(result.rows[0]);
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// 글 삭제 (소프트 삭제 + 댓글 소프트 삭제)
router.delete('/:id', async (req, res) => {
  const postId = req.params.id;
  try {
    // 게시글 존재 여부 확인
    const postCheck = await pool.query('SELECT * FROM posts WHERE id = $1', [postId]);
    if (!postCheck.rows.length) return res.status(404).json({ error: '글을 찾을 수 없습니다.' });
    if (postCheck.rows[0].is_deleted) return res.status(400).json({ error: '이미 삭제된 글입니다.' });

    // 게시글 소프트 삭제
    await pool.query('UPDATE posts SET is_deleted = TRUE WHERE id = $1', [postId]);

    // 댓글도 소프트 삭제
    await pool.query('UPDATE comments SET is_deleted = TRUE WHERE post_id = $1', [postId]);

    res.json({ message: '게시글과 해당 댓글이 삭제되었습니다.' });
  } catch (err) { res.status(500).json({ error: err.message }); }
});

// AI 글 작성
router.post('/ai/create', async (req, res) => {
  const { title, content, category } = req.body;
  if (!title || !content) return res.status(400).json({ error: '제목과 내용을 입력해주세요.' });
  if (category !== 'promotion_official' && category !== 'news') return res.status(400).json({ error: 'AI는 promotion_official 또는 news만 작성 가능합니다.' });

  try {
    const result = await pool.query(
      'INSERT INTO posts(author_name, title, content, category) VALUES($1,$2,$3,$4) RETURNING *',
      ['AI', title, content, category]
    );
    res.json(result.rows[0]);
  } catch (err) { res.status(500).json({ error: err.message }); }
});

module.exports = router;