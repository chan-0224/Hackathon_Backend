module.exports = {
  paginate(array, page = 1, pageSize = 10) {
    const offset = (page - 1) * pageSize;
    const paginatedItems = array.slice(offset, offset + pageSize);
    return {
      page,
      pageSize,
      total: array.length,
      totalPages: Math.ceil(array.length / pageSize),
      data: paginatedItems
    };
  },
  search(array, keyword, field) {
    if (!keyword) return array;
    return array.filter(item => item[field]?.toLowerCase().includes(keyword.toLowerCase()));
  }
};