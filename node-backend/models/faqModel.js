const pool = require('./db');

module.exports = {
  async getAllFaqs() {
    const result = await pool.query('SELECT * FROM faq ORDER BY id ASC');
    return result.rows;
  },

  async createFaq(question, answer, category) {
    const result = await pool.query(
      'INSERT INTO faq(question, answer, category) VALUES($1, $2, $3) RETURNING *',
      [question, answer, category]
    );
    return result.rows[0];
  }
};