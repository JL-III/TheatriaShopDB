import React from 'react';
import TablePagination from '@material-ui/core/TablePagination';
import IconButton from '@material-ui/core/IconButton';
import FirstPageIcon from '@material-ui/icons/FirstPage';
import KeyboardArrowLeft from '@material-ui/icons/KeyboardArrowLeft';
import KeyboardArrowRight from '@material-ui/icons/KeyboardArrowRight';
import LastPageIcon from '@material-ui/icons/LastPage';

import './top-pagination.css';

export const PaginationActions = ({
  count,
  page,
  rowsPerPage,
  onChangePage,
}) => {
  const handleFirstPageButtonClick = (event) => {
    onChangePage(event, 0);
  };

  const handleBackButtonClick = (event) => {
    onChangePage(event, page - 1);
  };

  const handleNextButtonClick = (event) => {
    onChangePage(event, page + 1);
  };

  const handleLastPageButtonClick = (event) => {
    onChangePage(event, Math.max(0, Math.ceil(count / rowsPerPage) - 1));
  };

  return (
    <div className='flex'>
      <IconButton
        onClick={handleFirstPageButtonClick}
        disabled={page === 0}
        aria-label='first page'
      >
        <FirstPageIcon />
      </IconButton>

      <IconButton
        onClick={handleBackButtonClick}
        disabled={page === 0}
        aria-label='previous page'
      >
        <KeyboardArrowLeft />
      </IconButton>

      <IconButton
        onClick={handleNextButtonClick}
        disabled={page >= Math.ceil(count / rowsPerPage - 1)}
        aria-label='next page'
      >
        <KeyboardArrowRight />
      </IconButton>

      <IconButton
        onClick={handleLastPageButtonClick}
        disabled={page >= Math.ceil(count / rowsPerPage) - 1}
        aria-label='last page'
      >
        <LastPageIcon />
      </IconButton>
    </div>
  );
};

export const TopPagination = ({
  page,
  setPage,
  count,
  labelTextEnd,
  loading,
}) => {
  if (loading) {
    return <div className='top-pagination pt-2'>Loading...</div>;
  }

  return (
    <div className='top-pagination'>
      <TablePagination
        component='div'
        count={count}
        page={page - 1}
        onChangePage={setPage}
        rowsPerPage={6}
        rowsPerPageOptions={[10]}
        labelDisplayedRows={({ from, to, count }) =>
          `${from} - ${to} of ${count} ${labelTextEnd}`
        }
        ActionsComponent={PaginationActions}
      />
    </div>
  );
};
