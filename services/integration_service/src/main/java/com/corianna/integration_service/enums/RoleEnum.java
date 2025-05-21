package com.corianna.integration_service.enums;

public enum RoleEnum {
    OWNER("owner"),
    ADMIN("admin"),
    MEMBER("member");

    private final String roleName;

    RoleEnum(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }
}