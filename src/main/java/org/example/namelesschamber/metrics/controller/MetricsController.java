package org.example.namelesschamber.metrics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.namelesschamber.common.response.ApiResponse;
import org.example.namelesschamber.metrics.dto.TodayMetricsDto;
import org.example.namelesschamber.metrics.service.MetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
@Tag(name = "Admin Metrics", description = "통계 API")
public class MetricsController {

    private final MetricsService metricsService;

    @Operation(summary = "오늘 metrics 조회")
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayMetricsDto>> getTodayMetrics() {
        TodayMetricsDto metrics = metricsService.getTodayMetrics();
        return ApiResponse.success(HttpStatus.OK, metrics);
    }

}
