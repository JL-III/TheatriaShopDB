import React, { useEffect, useState } from 'react';
import Tooltip from '@material-ui/core/Tooltip';

import './copy-button.css';

export const CopyButton = ({ text, copyText, className }) => {
  const [copied, setCopied] = useState(false);
  const [timeout, setCTimeout] = useState();

  useEffect(() => {
    return () => {
      if (timeout && copied) {
        clearTimeout(timeout);
      }
    };
  }, [timeout, copied]);

  const copy = (e) => {
    e.stopPropagation();
    navigator.clipboard.writeText(copyText);
    setCopied(true);
    setCTimeout(setTimeout(() => setCopied(false), 2000));
  };

  return (
    <Tooltip title={copied ? 'Copied!' : `Copy '${copyText}'`} placement='top'>
      <span className={`copy-button ${className}`} onClick={copy}>
        {text}
      </span>
    </Tooltip>
  );
};
