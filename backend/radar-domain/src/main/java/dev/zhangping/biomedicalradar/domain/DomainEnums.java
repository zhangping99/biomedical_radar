package dev.zhangping.biomedicalradar.domain;

public final class DomainEnums {
    private DomainEnums() {
    }

    public enum SourceTier { A, B, C }

    public enum SourceType { regulator, registry, journal, company, exchange, media }

    public enum Region { CN, US, EU, JP, GLOBAL, OTHER }

    public enum Category {
        policy_regulation,
        drug_rd,
        research_academic,
        pharma_biotech,
        hospital_service,
        device_diagnostics,
        capital_transactions,
        reimbursement_access,
        manufacturing_supply,
        quality_safety,
        public_health,
        frontier_technology
    }

    public enum EntityType { company, drug, disease, hospital, institution, person, other }

    public enum VerificationStatus { unreviewed, auto_checked, human_verified, rejected }

    public enum OriginalAccessStatus { unknown, reachable, slow, unreachable }

    public enum LegalBasis { official_api, rss_allowed, public_notice, licensed, manual_link }
}
