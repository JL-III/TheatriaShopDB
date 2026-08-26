import React from 'react';

// Vanilla legacy color codes -> hex.
const COLORS = {
  0: '#000000',
  1: '#0000AA',
  2: '#00AA00',
  3: '#00AAAA',
  4: '#AA0000',
  5: '#AA00AA',
  6: '#FFAA00',
  7: '#AAAAAA',
  8: '#555555',
  9: '#5555FF',
  a: '#55FF55',
  b: '#55FFFF',
  c: '#FF5555',
  d: '#FF55FF',
  e: '#FFFF55',
  f: '#FFFFFF',
};

const SECTION = '§';

const freshState = () => ({
  color: null,
  bold: false,
  italic: false,
  underline: false,
  strike: false,
});

// Parses a legacy §-formatted string (named colors, formatting codes, and
// §x§r§r§g§g§b§b hex sequences) into styled segments.
export const parseMcText = (text) => {
  const segments = [];
  let state = freshState();
  let buffer = '';

  const flush = () => {
    if (buffer) {
      segments.push({ ...state, text: buffer });
      buffer = '';
    }
  };

  for (let i = 0; i < text.length; i++) {
    const ch = text[i];
    if (ch !== SECTION || i + 1 >= text.length) {
      buffer += ch;
      continue;
    }

    const code = text[i + 1].toLowerCase();

    if (code === 'x' && i + 13 < text.length) {
      let hex = '';
      let valid = true;
      for (let j = 0; j < 6; j++) {
        const idx = i + 2 + j * 2;
        if (text[idx] !== SECTION) {
          valid = false;
          break;
        }
        hex += text[idx + 1];
      }
      if (valid && /^[0-9a-f]{6}$/i.test(hex)) {
        flush();
        state = { ...freshState(), color: `#${hex}` };
        i += 13;
        continue;
      }
    }

    if (COLORS[code]) {
      // A color code resets formatting, like the vanilla client.
      flush();
      state = { ...freshState(), color: COLORS[code] };
    } else if (code === 'l') {
      flush();
      state = { ...state, bold: true };
    } else if (code === 'o') {
      flush();
      state = { ...state, italic: true };
    } else if (code === 'n') {
      flush();
      state = { ...state, underline: true };
    } else if (code === 'm') {
      flush();
      state = { ...state, strike: true };
    } else if (code === 'r') {
      flush();
      state = freshState();
    }
    // Unknown codes (and §k obfuscation) are dropped; their text still renders.
    i++;
  }

  flush();
  return segments;
};

export const McText = ({ text }) => {
  if (!text) {
    return null;
  }
  return (
    <>
      {parseMcText(text).map((segment, i) => {
        const style = {};
        if (segment.color) style.color = segment.color;
        if (segment.bold) style.fontWeight = 'bold';
        if (segment.italic) style.fontStyle = 'italic';
        const decorations = [
          segment.underline && 'underline',
          segment.strike && 'line-through',
        ].filter(Boolean);
        if (decorations.length) style.textDecoration = decorations.join(' ');
        return (
          <span key={i} style={style}>
            {segment.text}
          </span>
        );
      })}
    </>
  );
};
