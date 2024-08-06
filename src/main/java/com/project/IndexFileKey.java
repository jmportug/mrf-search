package com.project;

public enum IndexFileKey {
    REPORTING_PLANS("reporting_plans"),
    PLAN_ID_TYPE("plan_id_type"),
    PLAN_ID("plan_id");


    private final String keyName;
    private IndexFileKey(String keyName) {
        this.keyName = keyName;
    }

    public String getKeyName() {
        return keyName;
    }
}
