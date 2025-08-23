const pool = require('./db');

module.exports = {
  async getCommentsByPost(post_id) {
    const result = await pool.query(
      'SELECT * FROM comments WHERE post_id=$1 ORDER BY created_at ASC',
      [post_id]
    );
    return result.rows;
  },
  async createComment(post_id, author_name, content) {
    const result = await pool.query(
      'INSERT INTO comments(post_id, author_name, content) VALUES($1,$2,$3) RETURNING *',
      [post_id, author_name, content]
    );
    return result.rows[0];
  },
  async updateComment(id, content) {
    const result = await pool.query(
      'UPDATE comments SET content=$1, updated_at=NOW() WHERE id=$2 RETURNING *',
      [content, id]
    );
    return result.rows[0];
  },
  async deleteComment(id) {
    const result = await pool.query('DELETE FROM comments WHERE id=$1 RETURNING *', [id]);
    return result.rows[0];
  }
};