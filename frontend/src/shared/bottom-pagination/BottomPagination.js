import React from 'react';
import Pagination from '@material-ui/lab/Pagination';

import './bottom-pagination.css';

export const BottomPagination = ({ page, totalPages, setPage }) => {
  return (
    <div className='bottom-pagination flex pt-3 pb-5'>
      <Pagination
        page={page}
        count={totalPages}
        onChange={setPage}
        variant='outlined'
        shape='rounded'
      />
    </div>
  );
};
