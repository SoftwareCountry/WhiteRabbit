package com.arcadia.whiteRabbitService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class DelimitedTextFileSettingsDto {
    private final String fileType;

    private final String delimiter;

    private final List<FileToScanDto> filesToScan;

    private final ScanParamsDto scanParameters;
}