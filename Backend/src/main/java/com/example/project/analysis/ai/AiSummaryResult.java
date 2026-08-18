package com.example.project.analysis.ai;

import java.util.List;

public record AiSummaryResult(String summary, List<String> technicalAreas, String model, String promptVersion) {
}
