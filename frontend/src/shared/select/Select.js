import React from 'react';
import ReactSelect from 'react-select';
import { FixedSizeList } from 'react-window';

import './select.css';

const colorStyles = {
  control: (styles, { isFocused }) => ({
    ...styles,
    backgroundColor: 'rgb(14,14,23)',
    border: isFocused ? '1px solid #f85102' : 0,
    boxShadow: 'none',
    ':hover': {
      border: isFocused ? '1px solid #f85102' : 0,
    },
  }),
  option: (styles, { data, isDisabled, isFocused, isSelected }) => ({
    backgroundColor: 'rgb(14,14,23)',
    fontWeight: 300,
    paddingLeft: '10px',
    paddingTop: '5px',
    paddingBottom: '5px',
    color: 'rgba(255, 255, 255, 0.6)',
    ':hover': {
      ...styles[':hover'],
      backgroundColor: '#323232',
      cursor: 'pointer',
    },
  }),
  menu: (styles) => ({ ...styles, backgroundColor: 'rgb(14,14,23)' }),
  input: (styles) => ({ ...styles, color: 'rgba(255, 255, 255, 0.6)' }),
  placeholder: (styles) => ({ ...styles, color: 'rgba(255, 255, 255, 0.6)' }),
  singleValue: (styles, { data }) => ({
    ...styles,
    color: 'rgba(255, 255, 255, 0.6)',
  }),
};

const MenuList = ({ options, children, maxHeight, getValue }) => {
  const height = 30;
  const [value] = getValue();
  const initialOffset = options.indexOf(value) * height;

  const getMaxHeight = () => {
    if (children.length === undefined) return 50;
    if (children.length >= 10) return maxHeight;
    return maxHeight - height * (10 - children.length) + 5;
  };

  return (
    <FixedSizeList
      height={getMaxHeight()}
      itemCount={children.length}
      itemSize={height}
      initialScrollOffset={initialOffset}
    >
      {({ index, style }) => <div style={style}>{children[index]}</div>}
    </FixedSizeList>
  );
};

export const Select = ({
  className,
  label,
  options,
  value,
  setValue,
  isSearchable,
  placeholder,
  loading,
  onFocus,
  windowed,
  isClearable,
}) => {
  return (
    <ReactSelect
      className={className}
      label={label}
      options={options}
      value={value}
      onChange={setValue}
      isSearchable={isSearchable}
      isClearable={isClearable}
      styles={colorStyles}
      placeholder={placeholder}
      isLoading={loading}
      onFocus={onFocus}
      components={windowed ? { MenuList } : undefined}
    />
  );
};
