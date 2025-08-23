const express = require('express');
const router = express.Router();
const pool = require('../models/db'); // pg Pool


// 마이페이지 조회
router.get('/', async (req, res) => {
  try {
    // 게시글 조회 (추후 사용자별 필터링 가능: WHERE user_id = $1)
    const postsResult = await pool.query('SELECT * FROM posts ORDER BY created_at DESC');
    const posts = postsResult.rows;

    // 댓글 조회
    const commentsResult = await pool.query('SELECT * FROM comments ORDER BY created_at DESC');
    const comments = commentsResult.rows;

    // 좋아요 총합 조회
    const likesResult = await pool.query('SELECT COUNT(*) AS total_likes FROM likes');
    const likes = likesResult.rows[0]?.total_likes || 0;

    // 관심 이벤트 조회
    const eventsResult = await pool.query('SELECT * FROM interested_events ORDER BY event_date');
    const interested_events = eventsResult.rows;

    // 프로필 조회 (현재는 단일 프로필, 추후 사용자별로 확장 가능)
    const profileResult = await pool.query('SELECT * FROM profile LIMIT 1');
    if (!profileResult.rows[0]) {
      return res.status(404).json({ error: "프로필 정보가 없습니다." });
    }
    const profile = profileResult.rows[0];

    // 최종 응답
    res.json({
      profile,
      posts,
      comments,
      likes,
      interested_events
    });

  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});


// 프로필 수정
router.put('/mypage/profile', async (req, res) => {
  const { nickname, bio, avatar_url } = req.body;

  if (!nickname || !bio || !avatar_url) {
    return res.status(400).json({ error: "모든 필드(nickname, bio, avatar_url)를 입력해야 합니다." });
  }

  try {
    // 프로필 업데이트 (id=1 고정)
    await pool.query(
      'UPDATE profile SET nickname = $1, bio = $2, avatar_url = $3 WHERE id = 1',
      [nickname, bio, avatar_url]
    );

    // 업데이트 후 최신 프로필 조회
    const profileResult = await pool.query('SELECT * FROM profile WHERE id = 1');
    const profile = profileResult.rows[0];

    res.json(profile);

  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

module.exports = router;