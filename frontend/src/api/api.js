import moment from 'moment';

export const parseResponse = (response) => {
  return response.json().then((response) => {
    if (response.error) {
      throw new Error(response.message);
    } else {
      return response;
    }
  });
};

export const getTimeFromNow = (timestamp) => {
  if (timestamp === null || timestamp === undefined) return 'never';
  return moment(timestamp).fromNow();
};
