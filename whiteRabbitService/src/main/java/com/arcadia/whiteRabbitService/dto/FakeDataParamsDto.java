package com.arcadia.whiteRabbitService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
public class FakeDataParamsDto {

    private final Integer maxRowCount;

    private final Boolean doUniformSampling;

    private final DbSettingsDto dbSettings;

    @Setter
    private String scanReportFileName;

    @Setter
    private String directory;
}
