const pool = require('./db');

module.exports = {
  async getAllFaqs() {
    const result = await pool.query('SELECT * FROM faq ORDER BY id ASC');
    return result.rows;
  },
  async createFaq(question, answer) {
    const result = await pool.query(
      'INSERT INTO faq(question, answer) VALUES($1,$2) RETURNING *',
      [question, answer]
    );
    return result.rows[0];
  }
};