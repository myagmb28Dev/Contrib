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
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!isOpen) return;

    setLoading(true);
    setError("");
    setSelectedIds(alreadySyncedRepoIds);

    getAvailableGitHubRepositories()
      .then((repos) => {
        setAvailableRepos(repos);
      })
      .catch((err: unknown) => {
        setError(
          err instanceof ApiRequestError
            ? err.message
            : "GitHub 저장소 목록을 불러오지 못했습니다."
        );
      })
      .finally(() => setLoading(false));
  }, [isOpen, alreadySyncedRepoIds]);

  if (!isOpen) return null;

  const filteredRepos = availableRepos.filter((repo) => {
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
      <div className="modal-dialog" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div>
            <h3>GitHub 저장소 선택 동기화</h3>
            <p className="muted">
              Contrib에서 분석 및 인증서를 생성할 GitHub 공개 저장소를 선택해 주세요.
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
            <input
              type="text"
              placeholder="저장소 이름 또는 언어로 검색..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="search-input"
              style={{ flex: 1 }}
            />
            <button
              type="button"
              className="button sm"
              onClick={selectAll}
              disabled={loading || filteredRepos.length === 0}
            >
              전체 선택
            </button>
            <button
              type="button"
              className="button sm"
              onClick={deselectAll}
              disabled={loading || selectedIds.length === 0}
            >
              선택 해제
            </button>
          </div>

          {loading ? (
            <div className="loading-card" style={{ padding: "40px 0" }}>
              <div className="skeleton-line lg" />
              <div className="skeleton-line md" />
              <p className="muted">GitHub에서 공개 저장소 목록을 조회하는 중입니다...</p>
            </div>
          ) : availableRepos.length === 0 ? (
            <div className="empty-state-card" style={{ padding: "30px 0" }}>
              <h4>동기화 가능한 공개 저장소가 없습니다</h4>
              <p className="muted">GitHub 계정에 공개(Public) 저장소가 있는지 확인해 주세요.</p>
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
          <button
            type="button"
            className="button"
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
              : newSelectionCount > 0
              ? `선택한 저장소 동기화 (${selectedIds.length}개, 신규 +${newSelectionCount})`
              : `선택한 저장소 동기화 (${selectedIds.length}개)`}
          </button>
        </div>
      </div>
    </div>
  );
}
