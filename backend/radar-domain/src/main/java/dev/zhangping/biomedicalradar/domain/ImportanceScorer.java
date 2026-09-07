package dev.zhangping.biomedicalradar.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.Category;
import static dev.zhangping.biomedicalradar.domain.DomainEnums.SourceTier;

public final class ImportanceScorer {
    private final Policy policy;

    public ImportanceScorer() {
        this(Policy.defaults());
    }

    public ImportanceScorer(Policy policy) {
        this.policy = policy;
    }

    public Score score(Article article, Instant now) {
        int value = switch (article.sourceTier()) {
            case A -> policy.tierA();
            case B -> policy.tierB();
            case C -> policy.tierC();
        };
        List<String> reasons = new ArrayList<>();
        if (article.sourceTier() == SourceTier.A) {
            reasons.add("官方一手来源");
        }
        long importantEvents = article.eventTypes().stream().filter(policy.highValueEvents()::contains).count();
        if (importantEvents > 0) {
            value += (int) Math.min(policy.highValueEventMaximum(), importantEvents * policy.highValueEvent());
            reasons.add("高影响事件");
        }
        if (article.category() == Category.policy_regulation || article.category() == Category.quality_safety) {
            value += policy.policyOrSafety();
            reasons.add("政策或安全相关");
        }
        Instant eventTime = article.publishedAt() == null ? article.collectedAt() : article.publishedAt();
        long hours = Math.max(0, Duration.between(eventTime, now).toHours());
        if (hours <= 24) {
            value += policy.publishedWithin24Hours();
            reasons.add("24小时内更新");
        } else if (hours <= 72) {
            value += policy.publishedWithin72Hours();
            reasons.add("近期更新");
        }
        if (article.sourceLinks().size() > 1) {
            value += Math.min(policy.additionalSourceMaximum(),
                    (article.sourceLinks().size() - 1) * policy.additionalSource());
            reasons.add("多来源交叉出现");
        }
        if (article.sourceTier() == SourceTier.C) {
            value = Math.min(value, policy.tierCMaximum());
            reasons.add("C级线索不进入今日必读");
        }
        return new Score(Math.min(100, value), reasons);
    }

    public record Score(int value, List<String> reasons) {
        public Score {
            reasons = List.copyOf(reasons);
        }
    }

    public record Policy(int tierA, int tierB, int tierC, int highValueEvent, int highValueEventMaximum,
                         int policyOrSafety, int publishedWithin24Hours, int publishedWithin72Hours,
                         int additionalSource, int additionalSourceMaximum, int mustReadMinimum,
                         int tierCMaximum, Set<String> highValueEvents) {
        public Policy {
            highValueEvents = Set.copyOf(highValueEvents);
        }

        public static Policy defaults() {
            return new Policy(35, 20, 0, 15, 30, 15, 15, 8, 5, 10, 60, 39,
                    Set.of("approval", "safety_alert", "phase_3_result", "policy_release", "recall"));
        }
    }
}
