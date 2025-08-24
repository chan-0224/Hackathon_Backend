const express = require('express');
const path = require('path');
const { Pool } = require('pg');
require('dotenv').config();

const app = express();
const port = 3001;

// PostgreSQL 연결
const pool = new Pool({
  user: process.env.DB_USER,
  host: process.env.DB_HOST,
  database: process.env.DB_NAME,
  password: process.env.DB_PASSWORD,
  port: process.env.DB_PORT,
});

// 미들웨어 설정
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// 업로드 이미지 접근용
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 라우터 연결
app.use('/posts', require('./routes/posts'));       // 게시판
app.use('/comments', require('./routes/comments')); // 댓글
app.use('/likes', require('./routes/likes'));       // 좋아요
app.use('/faq', require('./routes/faq'));           // FAQ
app.use('/notice', require('./routes/notice'));     // 공지사항
app.use('/events', require('./routes/events'));     // 이벤트
app.use('/mypage', require('./routes/mypage'));     // 마이페이지
app.use('/region', require('./routes/region'));     // 지역 설정 (POST /events 포함)

// 기본 테스트 라우트
app.get("/", async (req, res) => {
  try {
    const result = await pool.query("SELECT NOW()");
    res.send(result.rows[0]);
  } catch (err) {
    console.error(err);
    res.status(500).send("DB 연결 실패");
  }
});

// mypage 글 조회 (author_name으로 필터링)
app.get("/mypage/posts", async (req, res) => {
  try {
    const { author_name } = req.query;

    if (!author_name) {
      return res.status(400).json({ error: "author_name 쿼리 파라미터가 필요합니다." });
    }

    const result = await pool.query(
      "SELECT * FROM posts WHERE author_name = $1 ORDER BY created_at DESC",
      [author_name]
    );

    res.json(result.rows);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "서버 오류" });
  }
});

// 서버 실행
app.listen(port, () => {
  console.log(`Server running at http://localhost:${port}`);
});

// pool을 외부에서도 사용 가능하게 export
module.exports = { app, pool };