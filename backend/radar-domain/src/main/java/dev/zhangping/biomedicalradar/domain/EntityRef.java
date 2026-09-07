package dev.zhangping.biomedicalradar.domain;

import static dev.zhangping.biomedicalradar.domain.DomainEnums.EntityType;

public record EntityRef(EntityType type, String name) {
}
