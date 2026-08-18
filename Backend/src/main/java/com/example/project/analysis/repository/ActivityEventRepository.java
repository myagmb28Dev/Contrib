package com.example.project.analysis.repository;

import java.util.List;
import java.util.UUID;

import com.example.project.analysis.domain.ActivityEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {
    List<ActivityEvent> findAllBySnapshotIdOrderByOccurredAtAscExternalIdAsc(UUID snapshotId);
    void deleteAllBySnapshotId(UUID snapshotId);
}
