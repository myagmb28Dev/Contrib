"use client";

import { useEffect, useState } from "react";

interface TypewriterTextProps {
  text: string;
  speed?: number;
  className?: string;
}

export function TypewriterText({ text, speed = 35, className }: TypewriterTextProps) {
  const [displayed, setDisplayed] = useState("");
  const [index, setIndex] = useState(0);
  const [replayKey, setReplayKey] = useState(0);

  useEffect(() => {
    setDisplayed("");
    setIndex(0);
  }, [text, replayKey]);

  useEffect(() => {
    if (index < text.length) {
      const timer = setTimeout(() => {
        setDisplayed((prev) => prev + text.charAt(index));
        setIndex((prev) => prev + 1);
      }, speed);
      return () => clearTimeout(timer);
    }
  }, [index, text, speed, replayKey]);

  function handleReplay() {
    setReplayKey((prev) => prev + 1);
  }

  return (
    <span
      className={`typewriter-interactive ${className ?? ""}`}
      onClick={handleReplay}
      title="클릭하면 타이핑 애니메이션이 처음부터 다시 재생됩니다"
    >
      {displayed}
      <span className="typing-cursor" aria-hidden="true" />
    </span>
  );
}
