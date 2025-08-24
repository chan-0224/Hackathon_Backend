const pool = require('./db');

const LikeModel = {
  // 좋아요 추가
  addLike: async (post_id, author_name) => {
    const result = await pool.query(
      'INSERT INTO likes(post_id, author_name) VALUES($1,$2) ON CONFLICT DO NOTHING RETURNING *',
      [post_id, author_name]
    );
    return result.rows[0];
  },

  // 좋아요 삭제
  removeLike: async (post_id, author_name) => {
    const result = await pool.query(
      'DELETE FROM likes WHERE post_id=$1 AND author_name=$2 RETURNING *',
      [post_id, author_name]
    );
    return result.rows[0];
  },

  // 특정 글 좋아요 수 조회
  countLikes: async (post_id) => {
    const result = await pool.query(
      'SELECT COUNT(*) FROM likes WHERE post_id=$1',
      [post_id]
    );
    return parseInt(result.rows[0].count);
  },

  // 특정 사용자가 글을 좋아요 했는지 확인
  isLikedByUser: async (post_id, author_name) => {
    const result = await pool.query(
      'SELECT * FROM likes WHERE post_id=$1 AND author_name=$2',
      [post_id, author_name]
    );
    return result.rows.length > 0;
  }
};

module.exports = LikeModel;