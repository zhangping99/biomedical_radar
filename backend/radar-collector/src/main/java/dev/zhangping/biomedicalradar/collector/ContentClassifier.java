package dev.zhangping.biomedicalradar.collector;

import dev.zhangping.biomedicalradar.domain.DomainEnums.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ContentClassifier {
    public Category category(SourceDefinition source, RawSourceItem item) {
        String text = searchable(item);
        if (contains(text, "recall", "safety", "adverse", "warning", "召回", "安全", "不良反应")) {
            return Category.quality_safety;
        }
        if (contains(text, "policy", "regulation", "guidance", "法规", "政策", "指导原则")) {
            return Category.policy_regulation;
        }
        if (contains(text, "trial", "phase i", "phase ii", "phase iii", "clinical", "临床", "试验")) {
            return Category.drug_rd;
        }
        if (contains(text, "device", "diagnostic", "ivd", "器械", "诊断")) {
            return Category.device_diagnostics;
        }
        if (contains(text, "hospital", "medical service", "医院", "医疗服务")) {
            return Category.hospital_service;
        }
        if (contains(text, "funding", "acquisition", "merger", "filed", "8-k", "融资", "并购", "交易")) {
            return Category.capital_transactions;
        }
        if (contains(text, "reimbursement", "insurance", "医保", "支付")) {
            return Category.reimbursement_access;
        }
        if (contains(text, "manufacturing", "supply", "factory", "生产", "供应链")) {
            return Category.manufacturing_supply;
        }
        if (contains(text, "outbreak", "epidemic", "public health", "疫情", "公共卫生")) {
            return Category.public_health;
        }
        if (contains(text, "artificial intelligence", "machine learning", "crispr", "人工智能", "基因编辑")) {
            return Category.frontier_technology;
        }
        return switch (source.type()) {
            case regulator -> Category.policy_regulation;
            case registry -> Category.drug_rd;
            case journal -> Category.research_academic;
            case company, media -> Category.pharma_biotech;
            case exchange -> Category.capital_transactions;
        };
    }

    public List<String> eventTypes(RawSourceItem item) {
        String text = searchable(item);
        List<String> events = new ArrayList<>();
        addIf(events, text, "approval", "approved", "批准", "获批");
        addIf(events, text, "safety_alert", "safety", "warning", "安全警示", "不良反应");
        addIf(events, text, "recall", "recall", "召回");
        addIf(events, text, "phase_3_result", "phase iii", "phase 3", "三期", "Ⅲ期");
        addIf(events, text, "policy_release", "policy", "guidance", "政策", "指导原则");
        addIf(events, text, "clinical_registration", "clinical trial", "study registration", "临床试验");
        addIf(events, text, "filing", "filed", "8-k", "10-k", "10-q", "6-k", "20-f", "公告");
        addIf(events, text, "research_publication", "journal", "research", "study finds", "论文", "研究");
        return events.isEmpty() ? List.of("update") : List.copyOf(events);
    }

    private String searchable(RawSourceItem item) {
        return (item.title() + " " + (item.description() == null ? "" : item.description()))
                .toLowerCase(Locale.ROOT);
    }

    private boolean contains(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private void addIf(List<String> output, String value, String event, String... needles) {
        if (contains(value, needles)) {
            output.add(event);
        }
    }
}
