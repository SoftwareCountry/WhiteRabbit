package com.arcadia.whiteRabbitService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class DbSettingsDto implements SettingsDto {
    private final String dbType;

    private final String user;

    private final String password;

    private final String database;

    private final String server;

    private final Integer port;

    private final String schema;

    private final String domain;

    private final String tablesToScan;

    private final ScanParamsDto scanParams;

    @Override
    public void destroy() {
    }
}
