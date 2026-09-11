"use client";

import { useEffect, useState } from "react";
import {
  ApiRequestError,
  getAvailableGitHubRepositories,
  syncSelectedRepositories,
  type GitHubAvailableRepo,
  type Repository,
} from "@/lib/api";

type Props = {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (updatedRepos: Repository[]) => void;
  alreadySyncedRepoIds: number[];
};

export function SyncRepositoriesModal({
  isOpen,
  onClose,
  onSuccess,
  alreadySyncedRepoIds,
}: Props) {
  const [availableRepos, setAvailableRepos] = useState<GitHubAvailableRepo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [visibility, setVisibility] = useState("public");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isOpen) return;

    let ignore = false;

    getAvailableGitHubRepositories()
      .then((repos) => {
        if (!ignore) {
          setAvailableRepos(repos);
          setSelectedIds(alreadySyncedRepoIds);
          setError("");
        }
      })
      .catch((err: unknown) => {
        if (!ignore) {
          setError(
            err instanceof ApiRequestError
              ? err.message
              : "GitHub 저장소 목록을 불러오지 못했습니다."
          );
        }
      })
      .finally(() => {
        if (!ignore) setLoading(false);
      });

    return () => {
      ignore = true;
    };
  }, [isOpen, alreadySyncedRepoIds]);

  if (!isOpen) return null;

  const filteredRepos = availableRepos.filter((repo) => {
    if (visibility === "public" && repo.private) return false;
    if (visibility === "private" && !repo.private) return false;
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      repo.fullName.toLowerCase().includes(q) ||
      (repo.language && repo.language.toLowerCase().includes(q))
    );
  });

  function toggleSelect(id: number) {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((item) => item !== id) : [...prev, id]
    );
  }

  function selectAll() {
    const allFilteredIds = filteredRepos.map((r) => r.id);
    setSelectedIds((prev) => Array.from(new Set([...prev, ...allFilteredIds])));
  }

  function deselectAll() {
    const filteredIdSet = new Set(filteredRepos.map((r) => r.id));
    setSelectedIds((prev) => prev.filter((id) => !filteredIdSet.has(id)));
  }

  async function handleSubmit() {
    setSubmitting(true);
    setError("");
    try {
      const updated = await syncSelectedRepositories(selectedIds);
      onSuccess(updated);
      onClose();
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "선택한 저장소를 동기화하지 못했습니다."
      );
    } finally {
      setSubmitting(false);
    }
  }

  const newSelectionCount = selectedIds.filter(
    (id) => !alreadySyncedRepoIds.includes(id)
  ).length;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-dialog repo-sync-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3>GitHub 저장소 선택 동기화</h3>
            <p className="muted">
              Contrib에서 분석 및 인증서를 생성할 GitHub 저장소를 선택해 주세요.
            </p>
          </div>
          <button
            type="button"
            className="modal-close-btn"
            onClick={onClose}
            aria-label="닫기"
          >
            &times;
          </button>
        </div>

        <div className="modal-body">
          {error && <p className="error-message">{error}</p>}

          <div className="modal-toolbar">
            <select aria-label="저장소 공개 범위" className="search-input"
              value={visibility} onChange={(e) => setVisibility(e.target.value)}>
              <option value="public">Public (공개)</option>
              <option value="private">Private (비공개)</option>
              <option value="all">전체</option>
            </select>
            <input
              type="text"
              placeholder="저장소 이름 또는 언어로 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
              aria-label="저장소 검색"
            />
          </div>

          <div className="repo-selection-toolbar">
            <span className="repo-result-count">{loading ? "저장소 조회 중" : `${filteredRepos.length}개 저장소`}</span>
            <div className="repo-selection-actions">
              <button
                type="button"
                className="repo-text-action"
                onClick={selectAll}
                disabled={loading || submitting || filteredRepos.length === 0 || filteredRepos.every((repo) => selectedIds.includes(repo.id))}
              >
                전체 선택
              </button>
              <button
                type="button"
                className="repo-text-action"
                onClick={deselectAll}
                disabled={loading || submitting || !filteredRepos.some((repo) => selectedIds.includes(repo.id))}
              >
                선택 해제
              </button>
            </div>
          </div>

          {loading ? (
            <div className="loading-card" style={{ padding: "40px 0" }}>
              <div className="skeleton-line lg" />
              <div className="skeleton-line md" />
              <p className="muted">GitHub에서 저장소 목록을 조회하는 중입니다...</p>
            </div>
          ) : filteredRepos.length === 0 ? (
            <div className="empty-state-card" style={{ padding: "30px 0" }}>
              <h4>조건에 맞는 저장소가 없습니다</h4>
              <p className="muted">검색어나 공개 범위를 바꿔 주세요. Private 저장소가 보이지 않으면 다시 로그인해 GitHub 저장소 접근 권한을 허용해 주세요.</p>
            </div>
          ) : (
            <div className="modal-repo-list">
              {filteredRepos.map((repo) => {
                const isSelected = selectedIds.includes(repo.id);
                const isAlreadySynced = alreadySyncedRepoIds.includes(repo.id);

                return (
                  <div
                    key={repo.id}
                    className={`modal-repo-item ${isSelected ? "selected" : ""} ${
                      isAlreadySynced ? "already-synced" : ""
                    }`}
                    onClick={() => toggleSelect(repo.id)}
                  >
                    <div className="modal-item-left">
                      <div className="custom-check-box" aria-hidden="true">
                        {isSelected && (
                          <svg
                            viewBox="0 0 16 16"
                            width="12"
                            height="12"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2.8"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          >
                            <polyline points="3.5 8.5 6.5 11.5 12.5 4.5" />
                          </svg>
                        )}
                      </div>
                      <div className="modal-repo-info">
                        <span className="modal-repo-name">{repo.name}</span>
                        <span className="modal-repo-sub">{repo.fullName}</span>
                      </div>
                    </div>

                    <div className="modal-item-right">
                      <span className="visibility-badge">{repo.private ? "Private" : "Public"}</span>
                      {isAlreadySynced && (
                        <span className="synced-tag">동기화됨</span>
                      )}
                      {repo.language && (
                        <span className="lang-tag" style={{ marginLeft: 8 }}>
                          <span className="lang-dot" />
                          {repo.language}
                        </span>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="modal-footer">
          <div className="repo-selection-summary" role="status">
            <strong>{selectedIds.length}개 선택</strong>
            {newSelectionCount > 0 && <span>신규 {newSelectionCount}개</span>}
          </div>
          <div className="repo-footer-actions">
            <button
              type="button"
              className="button repo-cancel-button"
              onClick={onClose}
              disabled={submitting}
            >
              취소
            </button>
            <button
              type="button"
              className="button primary"
              onClick={handleSubmit}
              disabled={submitting || loading}
            >
              {submitting
                ? "동기화 적용 중..."
                : "선택한 저장소 동기화"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
