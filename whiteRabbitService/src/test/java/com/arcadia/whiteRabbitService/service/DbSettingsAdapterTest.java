package com.arcadia.whiteRabbitService.service;

import com.arcadia.whiteRabbitService.dto.DbSettingsDto;
import com.arcadia.whiteRabbitService.dto.ScanParamsDto;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.ohdsi.databases.DbType;
import org.ohdsi.whiteRabbit.DbSettings;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DbSettingsAdapterTest {

    @SneakyThrows
    @Test
    void testAdaptFields() {
        var dto = createTestDbSettingsDto("Sql Server", 1433);

        DbSettings dbSettings = DbSettingsAdapter.adaptDbSettings(dto);

        assertAll("Should be equals all string and primitive fields",
                () -> assertEquals(dto.getUser(), dbSettings.user),
                () -> assertEquals(dto.getPassword(), dbSettings.password),
                () -> assertEquals(dto.getDatabase(), dbSettings.database),
                () -> assertEquals(String.format("%s:%s", dto.getServer(), dto.getPort()), dbSettings.server)
        );
    }

    @SneakyThrows
    @Test
    void testAdaptMsSqlDbType() {
        var dto = createTestDbSettingsDto("SQL SERVER", 1433);

        DbSettings dbSettings = DbSettingsAdapter.adaptDbSettings(dto);

        assertEquals(dbSettings.dbType, DbType.MSSQL);
    }

    @SneakyThrows
    @Test
    void testAdaptPostgreDbType() {
        var dto = createTestDbSettingsDto("postgresql", 5432);

        DbSettings dbSettings = DbSettingsAdapter.adaptDbSettings(dto);

        assertEquals(dbSettings.dbType, DbType.POSTGRESQL);
    }

    private DbSettingsDto createTestDbSettingsDto(String dbType, int port) {
        String server = "822JNJ16S03V";
        String database = "CPRD";
        String user = "cdm_builder";
        String password = "builder1!";
        String tablesToScan = "dbo.lookup,dbo.medical";
        String domain = "";
        String schema = "";

        int sampleSize = (int) 100e3;
        int minCellCount = 5;
        int maxValues = 1000;
        int numericStatsSamplerSize = 500;

        return new DbSettingsDto(
                dbType,
                user, password,
                database, server, port, domain, schema,
                tablesToScan,
                new ScanParamsDto(
                        sampleSize, true,
                        minCellCount, maxValues,
                        false, numericStatsSamplerSize
                )
        );
    }
}
