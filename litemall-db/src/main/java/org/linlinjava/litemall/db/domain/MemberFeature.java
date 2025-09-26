package org.linlinjava.litemall.db.domain;

import java.util.List;

import lombok.Data;
@Data
public class MemberFeature {
    private Integer id;
    private String featureName;
    private String featureCode;
    private boolean enabled;
    private String description;
    private List<String> warnTips;
}
