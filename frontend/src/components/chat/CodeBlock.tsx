import React from 'react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';

interface CodeBlockProps {
  className?: string;
  children: React.ReactNode;
}

export function CodeBlock({ className, children, ...rest }: CodeBlockProps) {
  const match = /language-(\w+)/.exec(className || '');
  const language = match?.[1];
  const isInline = !className && !match;

  if (!isInline && language) {
    return (
      <SyntaxHighlighter
        style={vscDarkPlus}
        language={language}
        PreTag="div"
      >
        {String(children).replace(/\n$/, '')}
      </SyntaxHighlighter>
    );
  }

  return <code className={className} {...rest}>{children}</code>;
}
