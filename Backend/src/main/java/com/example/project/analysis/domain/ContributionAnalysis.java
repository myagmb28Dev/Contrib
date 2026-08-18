package com.example.project.analysis.domain;

import java.util.UUID;

import com.example.project.common.auditing.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "contribution_analysis")
public class ContributionAnalysis extends BaseTimeEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false, unique = true)
    private RepositorySnapshot snapshot;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String metrics;

    @Column(nullable = false)
    private int score;

    @Column(name = "score_version", nullable = false)
    private String scoreVersion;

    @Column(name = "calculation_rules", nullable = false, columnDefinition = "TEXT")
    private String calculationRules;

    @Column(name = "technical_areas", nullable = false, columnDefinition = "TEXT")
    private String technicalAreas;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_model")
    private String aiModel;

    @Column(name = "ai_prompt_version")
    private String aiPromptVersion;

    @Column(name = "ai_regeneration_count", nullable = false)
    private int aiRegenerationCount;

    protected ContributionAnalysis() {
    }

    public static ContributionAnalysis create(RepositorySnapshot snapshot, String metrics, int score,
            String scoreVersion, String calculationRules, String technicalAreas) {
        ContributionAnalysis analysis = new ContributionAnalysis();
        analysis.id = UUID.randomUUID();
        analysis.snapshot = snapshot;
        analysis.metrics = metrics;
        analysis.score = score;
        analysis.scoreVersion = scoreVersion;
        analysis.calculationRules = calculationRules;
        analysis.technicalAreas = technicalAreas;
        analysis.aiRegenerationCount = 0;
        return analysis;
    }

    public void applyAiSummary(String summary, String model, String promptVersion, String technicalAreas) {
        this.aiSummary = summary;
        this.aiModel = model;
        this.aiPromptVersion = promptVersion;
        this.technicalAreas = technicalAreas;
        this.aiRegenerationCount++;
    }

    public UUID getId() { return id; }
    public RepositorySnapshot getSnapshot() { return snapshot; }
    public String getMetrics() { return metrics; }
    public int getScore() { return score; }
    public String getScoreVersion() { return scoreVersion; }
    public String getCalculationRules() { return calculationRules; }
    public String getTechnicalAreas() { return technicalAreas; }
    public String getAiSummary() { return aiSummary; }
    public String getAiModel() { return aiModel; }
    public String getAiPromptVersion() { return aiPromptVersion; }
    public int getAiRegenerationCount() { return aiRegenerationCount; }
}
