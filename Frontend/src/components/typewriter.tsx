"use client";

import { useEffect, useState } from "react";

interface TypewriterTextProps {
  text: string;
  speed?: number;
  className?: string;
}

export function TypewriterText({ text, speed = 35, className }: TypewriterTextProps) {
  const [replayKey, setReplayKey] = useState(0);

  return (
    <TypewriterCore
      key={`${replayKey}-${text}`}
      text={text}
      speed={speed}
      className={className}
      onReplay={() => setReplayKey((k) => k + 1)}
    />
  );
}

function TypewriterCore({
  text,
  speed,
  className,
  onReplay,
}: {
  text: string;
  speed: number;
  className?: string;
  onReplay: () => void;
}) {
  const [displayed, setDisplayed] = useState("");

  useEffect(() => {
    let current = 0;
    const interval = setInterval(() => {
      current++;
      setDisplayed(text.slice(0, current));
      if (current >= text.length) {
        clearInterval(interval);
      }
    }, speed);
    return () => clearInterval(interval);
  }, [text, speed]);

  return (
    <span
      className={`typewriter-interactive ${className ?? ""}`}
      onClick={onReplay}
    >
      {displayed}
      <span className="typing-cursor" aria-hidden="true" />
    </span>
  );
}
