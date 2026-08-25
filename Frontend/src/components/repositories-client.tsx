"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

import { getRepositories, syncRepositories, type Repository } from "@/lib/api";

export function RepositoriesClient() {
  const [items, setItems] = useState<Repository[]>([]);
  const [message, setMessage] = useState("불러오는 중입니다...");
  const [syncing, setSyncing] = useState(false);

  useEffect(() => {
    getRepositories().then((value) => { setItems(value); setMessage(""); })
      .catch((error: Error) => setMessage(error.message));
  }, []);

  async function sync() {
    setSyncing(true);
    try {
      setItems(await syncRepositories());
      setMessage("");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "동기화하지 못했습니다.");
    } finally {
      setSyncing(false);
    }
  }

  return (
    <div className="stack full-width">
      <button className="button primary" onClick={sync} disabled={syncing}>
        {syncing ? "GitHub 동기화 중..." : "GitHub 저장소 동기화"}
      </button>
      {message && <p className="muted">{message}</p>}
      <div className="item-list">
        {items.map((repository) => (
          <Link className="list-card" key={repository.id} href={`/repositories/${repository.id}/overview`}>
            <strong>{repository.fullName}</strong>
            <span>{repository.language ?? "언어 미지정"} · {repository.defaultBranch}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
