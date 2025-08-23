const pool = require('./db');

const validCategories = ['free', 'promotion_personal', 'promotion_official', 'news', 'hot'];

const PostModel = {
  create: async ({ author_name = '익명', title, content, category = 'free', image_url = null }) => {
    const postCategory = validCategories.includes(category) ? category : 'free';
    const result = await pool.query(
      `INSERT INTO posts(author_name, title, content, category, image_url) 
       VALUES($1, $2, $3, $4, $5) RETURNING *`,
      [author_name, title, content, postCategory, image_url]
    );
    return result.rows[0];
  },

  findAll: async ({ category, page = 1, limit = 10, search, sort = 'desc' }) => {
    const offset = (page - 1) * limit;
    let baseQuery = 'SELECT * FROM posts';
    let params = [];
    const conditions = [];

    if (category && validCategories.includes(category) && category !== 'hot') {
      params.push(category);
      conditions.push(`category = $${params.length}`);
    }
    if (search) {
      params.push(`%${search}%`);
      conditions.push(`title ILIKE $${params.length}`);
    }
    if (conditions.length) baseQuery += ' WHERE ' + conditions.join(' AND ');

    if (category === 'hot') {
      baseQuery = 'SELECT * FROM posts ORDER BY likes DESC, views DESC LIMIT $1 OFFSET $2';
      params = [limit, offset];
    } else {
      params.push(limit, offset);
      baseQuery += ` ORDER BY created_at ${sort.toLowerCase() === 'asc' ? 'ASC' : 'DESC'} LIMIT $${params.length - 1} OFFSET $${params.length}`;
    }

    const result = await pool.query(baseQuery, params);
    return result.rows;
  },

  findById: async (id) => {
    const result = await pool.query('SELECT * FROM posts WHERE id=$1', [id]);
    return result.rows[0];
  },

  update: async ({ id, title, content, category = 'free', image_url = null }) => {
    const postCategory = validCategories.includes(category) ? category : 'free';
    let result;

    if (image_url) {
      result = await pool.query(
        `UPDATE posts SET title=$1, content=$2, category=$3, image_url=$4, updated_at=NOW() WHERE id=$5 RETURNING *`,
        [title, content, postCategory, image_url, id]
      );
    } else {
      result = await pool.query(
        `UPDATE posts SET title=$1, content=$2, category=$3, updated_at=NOW() WHERE id=$4 RETURNING *`,
        [title, content, postCategory, id]
      );
    }

    return result.rows[0];
  },

  delete: async (id) => {
    const result = await pool.query('DELETE FROM posts WHERE id=$1 RETURNING *', [id]);
    return result.rows[0];
  }
};

module.exports = PostModel;