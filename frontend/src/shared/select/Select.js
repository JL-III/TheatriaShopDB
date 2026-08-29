import React from 'react';
import ReactSelect, { components as reactSelectComponents } from 'react-select';
import CreatableSelect from 'react-select/creatable';
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
  option: (styles, { isDisabled, isFocused, isSelected }) => ({
    ...styles,
    backgroundColor: isSelected
      ? '#f85102'
      : isFocused
        ? '#323232'
        : 'rgb(14,14,23)',
    fontWeight: 300,
    paddingLeft: '10px',
    paddingTop: '5px',
    paddingBottom: '5px',
    color: isDisabled
      ? 'rgba(255, 255, 255, 0.3)'
      : isSelected
        ? '#171725'
        : 'rgba(255, 255, 255, 0.75)',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    ':hover': {
      ...styles[':hover'],
      backgroundColor: isSelected ? '#f85102' : '#323232',
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
  const menuItems = React.Children.toArray(children);
  const listRef = React.useRef(null);
  const [value] = getValue();
  const initialOffset = Math.max(0, options.indexOf(value)) * height;
  const focusedIndex = menuItems.findIndex(
    (item) => item && item.props && item.props.isFocused
  );

  React.useEffect(() => {
    if (focusedIndex >= 0 && listRef.current) {
      listRef.current.scrollToItem(focusedIndex);
    }
  }, [focusedIndex]);

  const getMaxHeight = () => {
    if (menuItems.length >= 10) return maxHeight;
    return Math.max(50, menuItems.length * height + 5);
  };

  return (
    <FixedSizeList
      ref={listRef}
      height={getMaxHeight()}
      itemCount={menuItems.length}
      itemSize={height}
      initialScrollOffset={initialOffset}
    >
      {({ index, style }) => <div style={style}>{menuItems[index]}</div>}
    </FixedSizeList>
  );
};

// react-select v3 hides its single-select input after an option is chosen.
// The chest-shop omnibox deliberately keeps the query in inputValue instead
// of rendering a selected token, so its input must remain visible as well.
const AlwaysVisibleInput = (props) => (
  <reactSelectComponents.Input {...props} isHidden={false} />
);

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
  creatable,
  allowCreateWhileLoading,
  createOptionPosition,
  formatCreateLabel,
  isValidNewOption,
  onCreateOption,
  inputValue,
  onInputChange,
  filterOption,
  formatOptionLabel,
  controlShouldRenderValue,
  selectInputTextOnFocus,
  keepInputVisible,
}) => {
  const SelectComponent = creatable ? CreatableSelect : ReactSelect;
  const creatableProps = creatable
    ? {
        createOptionPosition,
        formatCreateLabel,
        isValidNewOption,
        onCreateOption,
        allowCreateWhileLoading,
      }
    : {};
  const componentOverrides = {};
  if (windowed) componentOverrides.MenuList = MenuList;
  if (keepInputVisible) componentOverrides.Input = AlwaysVisibleInput;

  return (
    <SelectComponent
      className={className}
      classNamePrefix="theatria-select"
      aria-label={label}
      options={options}
      value={value}
      onChange={setValue}
      isSearchable={isSearchable}
      isClearable={isClearable}
      styles={colorStyles}
      placeholder={placeholder}
      isLoading={loading}
      onFocus={onFocus}
      components={
        Object.keys(componentOverrides).length ? componentOverrides : undefined
      }
      inputValue={inputValue}
      onInputChange={onInputChange}
      filterOption={filterOption}
      formatOptionLabel={formatOptionLabel}
      controlShouldRenderValue={controlShouldRenderValue}
      selectInputTextOnFocus={selectInputTextOnFocus}
      {...creatableProps}
    />
  );
};
