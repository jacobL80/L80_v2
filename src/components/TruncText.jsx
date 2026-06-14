import React, { useRef, useLayoutEffect, useState } from 'react';

const TruncText = ({ as: Tag = 'div', tipId, content, className, style, children }) => {
  const ref = useRef(null);
  const [show, setShow] = useState(false);

  useLayoutEffect(() => {
    const el = ref.current;
    if (!el) return;
    const check = () => setShow(el.scrollWidth > el.clientWidth);
    check();
    const ro = new ResizeObserver(check);
    ro.observe(el);
    return () => ro.disconnect();
  }, [content]);

  return (
    <Tag
      ref={ref}
      className={className}
      style={style}
      {...(show ? { 'data-tooltip-id': tipId, 'data-tooltip-content': content } : {})}
    >
      {children}
    </Tag>
  );
};

export default TruncText;
