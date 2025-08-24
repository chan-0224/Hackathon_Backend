const express = require('express');
const router = express.Router();
const pool = require('../models/db');


// 이벤트 생성 (POST)
router.post('/', async (req, res) => {
  const { title, description, start_date, end_date, location, is_public, source_post_id } = req.body;

  if (!title || !start_date || !end_date) {
    return res.status(400).json({ error: "필수값 누락" });
  }

  try {
    const result = await pool.query(
      `INSERT INTO events(title, description, start_date, end_date, location, is_public, source_post_id)
       VALUES($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [title, description, start_date, end_date, location, is_public !== false, source_post_id || null]
    );
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});


// 이벤트 목록 조회
// 옵션: 공개 여부, 진행중/다가오는 일정
router.get('/', async (req, res) => {
  const { public_only, upcoming_or_ongoing } = req.query;
  const today = new Date().toISOString().split('T')[0];

  try {
    let query = 'SELECT * FROM events';
    const whereClauses = [];
    const params = [];

    // 공개 이벤트만
    if (public_only === 'true') whereClauses.push('is_public = true');

    // 진행중 혹은 다가오는 이벤트
    if (upcoming_or_ongoing === 'true') {
      params.push(today);
      whereClauses.push(`end_date >= $${params.length}`);
    }

    if (whereClauses.length) query += ' WHERE ' + whereClauses.join(' AND ');

    // 날짜 순 정렬
    query += ' ORDER BY start_date ASC';

    const result = await pool.query(query, params);
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});


// 이벤트 상세 조회 (GET /:id)
router.get('/:id', async (req, res) => {
  const { id } = req.params;
  try {
    const result = await pool.query('SELECT * FROM events WHERE id=$1', [id]);
    if (!result.rows.length) return res.status(404).json({ error: "이벤트 없음" });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});


// 이벤트 수정 (PUT /:id)
router.put('/:id', async (req, res) => {
  const { id } = req.params;
  const { title, description, start_date, end_date, location, is_public, source_post_id } = req.body;

  try {
    const result = await pool.query(
      `UPDATE events
       SET title=$1, description=$2, start_date=$3, end_date=$4, location=$5, is_public=$6, source_post_id=$7, updated_at=NOW()
       WHERE id=$8
       RETURNING *`,
      [title, description, start_date, end_date, location, is_public !== false, source_post_id || null, id]
    );
    if (!result.rows.length) return res.status(404).json({ error: "이벤트 없음" });
    res.json(result.rows[0]);
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 이벤트 삭제 (DELETE /:id)

router.delete('/:id', async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM events WHERE id=$1 RETURNING *',
      [req.params.id]
    );
    if (!result.rows.length) return res.status(404).json({ error: "이벤트 없음" });
    res.json({ message: "삭제됨" });
  } catch (err) {
    res.status(500).json({ error: err.message });
  }
});

// 좋아요 / 관심 표시는 프론트에서 localStorage 등으로 구현


module.exports = router;