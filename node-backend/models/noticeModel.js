const pool = require('./db');

module.exports = {
  async getAllNotices() {
    const result = await pool.query('SELECT * FROM notice ORDER BY created_at DESC');
    return result.rows;
  },
  async createNotice(title, content) {
    const result = await pool.query(
      'INSERT INTO notice(title, content) VALUES($1,$2) RETURNING *',
      [title, content]
    );
    return result.rows[0];
  }
};