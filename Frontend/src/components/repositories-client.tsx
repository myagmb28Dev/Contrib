"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";

import { ApiRequestError, deleteRepository, getRepositories, syncRepositories, type Repository } from "@/lib/api";

export function RepositoriesClient() {
  const router = useRouter();
  const [items, setItems] = useState<Repository[]>([]);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [syncing, setSyncing] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    getRepositories()
      .then((value) => {
        setItems(value);
        setMessage("");
      })
      .catch((error: unknown) => {
        if (error instanceof ApiRequestError && error.status === 401) {
          router.replace("/");
          return;
        }
        setMessage(error instanceof Error ? error.message : "저장소 목록을 불러오지 못했습니다.");
      })
      .finally(() => setLoading(false));
  }, [router]);

  async function sync() {
    setSyncing(true);
    setMessage("");
    try {
      const updated = await syncRepositories();
      setItems(updated);
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) {
        router.replace("/");
        return;
      }
      setMessage(error instanceof Error ? error.message : "저장소 동기화에 실패했습니다.");
    } finally {
      setSyncing(false);
    }
  }

  async function handleUnsync(repoId: string, repoName: string) {
    if (!window.confirm(`'${repoName}' 저장소의 동기화를 해제하시겠습니까?`)) {
      return;
    }
    setDeletingId(repoId);
    try {
      await deleteRepository(repoId);
      setItems((prev) => prev.filter((r) => r.id !== repoId));
    } catch (error) {
      if (error instanceof ApiRequestError && error.status === 401) {
        router.replace("/");
        return;
      }
      setMessage(error instanceof Error ? error.message : "동기화 해제에 실패했습니다.");
    } finally {
      setDeletingId(null);
    }
  }

  const filteredItems = items.filter((repo) => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return true;
    return (
      repo.fullName.toLowerCase().includes(query) ||
      (repo.language && repo.language.toLowerCase().includes(query))
    );
  });

  return (
    <div className="stack full-width">
      {/* Top Toolbar */}
      <div className="toolbar-row">
        <div className="search-box">
          <input
            type="text"
            placeholder="저장소 이름 또는 언어 검색..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="search-input"
          />
          {searchQuery && (
            <button
              type="button"
              className="clear-search"
              onClick={() => setSearchQuery("")}
            >
              초기화
            </button>
          )}
        </div>

        <button
          className="button primary"
          onClick={sync}
          disabled={syncing}
        >
          {syncing ? "GitHub 저장소 동기화 중..." : "GitHub 저장소 동기화"}
        </button>
      </div>

      {message && <p className="error-message">{message}</p>}

      {loading ? (
        <div className="card loading-card">
          <div className="skeleton-line lg" />
          <div className="skeleton-line md" />
          <p className="muted">저장소 목록을 불러오는 중입니다...</p>
        </div>
      ) : items.length === 0 ? (
        <div className="card empty-state-card">
          <h3>동기화된 저장소가 없습니다</h3>
          <p className="muted">
            GitHub 계정의 공개 저장소를 동기화하여 기여도 분석을 시작하세요.
          </p>
          <button className="button primary" onClick={sync} disabled={syncing}>
            {syncing ? "동기화 중..." : "지금 저장소 동기화하기"}
          </button>
        </div>
      ) : filteredItems.length === 0 ? (
        <div className="card empty-state-card">
          <h3>검색 결과가 없습니다</h3>
          <p className="muted">&apos;{searchQuery}&apos;에 해당하는 저장소를 찾을 수 없습니다.</p>
          <button className="button sm" onClick={() => setSearchQuery("")}>
            검색어 초기화
          </button>
        </div>
      ) : (
        <div className="repository-grid">
          {filteredItems.map((repo) => (
            <article className="repository-card" key={repo.id}>
              <div className="repo-card-header">
                <div className="repo-card-title">
                  <Link
                    href={`/repositories/${repo.id}/overview`}
                    className="repo-name"
                    title={repo.fullName}
                  >
                    <strong>{repo.name}</strong>
                    <small className="muted">@{repo.ownerLogin}</small>
                  </Link>
                </div>
                <span className="visibility-badge">{repo.visibility}</span>
              </div>

              <div className="repo-meta-row">
                {repo.language && (
                  <span className="lang-tag">
                    <span className="lang-dot" />
                    {repo.language}
                  </span>
                )}
                <span className="branch-tag">
                  브랜치: {repo.defaultBranch}
                </span>
                {repo.archived && <span className="archived-tag">보관됨</span>}
              </div>

              <div className="repo-card-footer">
                <span className="sync-time muted">
                  동기화: {new Date(repo.lastSyncedAt).toLocaleDateString()}
                </span>
                <div className="repo-actions">
                  <Link
                    className="button primary sm"
                    href={`/repositories/${repo.id}/analyze`}
                  >
                    기여 분석
                  </Link>
                  <Link
                    className="button sm"
                    href={`/repositories/${repo.id}/overview`}
                  >
                    이력
                  </Link>
                  <button
                    type="button"
                    className="button danger-outline sm"
                    onClick={() => handleUnsync(repo.id, repo.name)}
                    disabled={deletingId === repo.id}
                    title="저장소 동기화 해제"
                  >
                    {deletingId === repo.id ? "해제 중..." : "동기화 해제"}
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  );
}
