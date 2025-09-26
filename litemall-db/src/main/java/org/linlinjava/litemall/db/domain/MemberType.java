package org.linlinjava.litemall.db.domain;

import java.util.List;

import lombok.Data;
@Data
public class MemberType {
    private Integer id;
    private String brief;
    private String name;
    private String goodsType;
    private boolean enabled;
    private List<String> warnTips;
    private List<MemberFeature> features;
}
