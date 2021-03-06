package com.arcadia.whiteRabbitService.service;

import com.arcadia.whiteRabbitService.dto.*;
import com.arcadia.whiteRabbitService.service.error.DbTypeNotSupportedException;
import com.arcadia.whiteRabbitService.service.error.FailedToGenerateFakeData;
import com.arcadia.whiteRabbitService.service.error.FailedToScanException;
import lombok.AllArgsConstructor;
import org.ohdsi.databases.RichConnection;
import org.ohdsi.utilities.Logger;
import org.ohdsi.whiteRabbit.DbSettings;
import org.ohdsi.whiteRabbit.scan.SourceDataScan;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import static com.arcadia.whiteRabbitService.service.DbSettingsAdapter.adaptDbSettings;
import static com.arcadia.whiteRabbitService.util.CompareDate.getDateDiffInHours;
import static com.arcadia.whiteRabbitService.util.DbSettingsUtil.dtoToDbSettings;
import static com.arcadia.whiteRabbitService.util.FileUtil.*;
import static java.lang.String.format;

@AllArgsConstructor
@Service
public class WhiteRabbitFacade {

    private final FakeDataService fakeDataService;

    /*
    * Scan report files creation dates
    * */
    private final Map<String, Date> scanReportCreationDates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // Create directory for scan-reports storage
        createDirectory(scanReportLocation);
    }

    public TestConnectionDto testConnection(DbSettingsDto dto) {
        try {
            DbSettings dbSettings = adaptDbSettings(dto);

            RichConnection connection = new RichConnection(dbSettings.server, dbSettings.domain,
                    dbSettings.user, dbSettings.password, dbSettings.dbType);

            var tableNames = connection.getTableNames(dbSettings.database);
            if (tableNames.size() == 0) {
                throw new Exception("Unable to retrieve table names for database " + dbSettings.database);
            }

            connection.close();

            var successMessage = format("Successfully connected to %s on server %s", dbSettings.database, dbSettings.server);
            return new TestConnectionDto(true, successMessage);
        } catch (Exception e) {
            var errorMessage = format("Could not connect to database: %s", e.getMessage());
            return new TestConnectionDto(false, errorMessage);
        }
    }

    public TablesInfoDto tablesInfo(DbSettingsDto dto) throws DbTypeNotSupportedException {
        DbSettings dbSettings = adaptDbSettings(dto);

        try (RichConnection connection = new RichConnection(
                dbSettings.server, dbSettings.domain,
                dbSettings.user, dbSettings.password,
                dbSettings.dbType
        )) {
            return new TablesInfoDto(connection.getTableNames(dbSettings.database));
        }
    }

    @Async
    public Future<String> generateScanReport(SettingsDto dto, Logger logger) throws FailedToScanException {
        try {
            DbSettings dbSettings = dtoToDbSettings(dto);
            SourceDataScan sourceDataScan = createSourceDataScan(dto.getScanParams(), logger);
            String scanReportFileName = generateRandomFileName();

            sourceDataScan.process(dbSettings, toScanReportFileFullName(scanReportFileName));

            return new AsyncResult<>(saveFileLocation(scanReportFileName));
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                logger.cancel(e.getMessage());
            } else {
                logger.error(e.getMessage());
            }
            FailedToScanException exception = new FailedToScanException(e);
            logger.failed(exception.getMessage());
            throw exception;
        } finally {
            dto.destroy();
        }
    }

    @Async
    public Future<Void> generateFakeData(FakeDataParamsDto dto, Logger logger) throws FailedToGenerateFakeData {
        fakeDataService.generateFakeData(dto, logger);
        return new AsyncResult<>(null);
    }

    private SourceDataScan createSourceDataScan(ScanParamsDto dto, Logger logger) {
        return new SourceDataScanBuilder()
                .setSampleSize(dto.getSampleSize())
                .setScanValues(dto.getScanValues())
                .setMinCellCount(dto.getMinCellCount())
                .setMaxValues(dto.getMaxValues())
                .setCalculateNumericStats(dto.getCalculateNumericStats())
                .setNumericStatsSamplerSize(dto.getNumericStatsSamplerSize())
                .setLogger(logger)
                .build();
    }

    private String saveFileLocation(String fileLocation) {
        scanReportCreationDates.put(fileLocation, new Date());
        return fileLocation;
    }

    /* 8 hours */
    @Scheduled(fixedRate = 1000 * 60 * 60 * 8)
    private void clearFileStorage() {
        Date currentDate = new Date();

        for (Map.Entry<String, Date> entry : scanReportCreationDates.entrySet()) {
            long diffInHours = getDateDiffInHours(currentDate, entry.getValue());

            if (diffInHours > 4) {
                File file = new File(entry.getKey());
                file.delete();
            }
        }
    }
}
