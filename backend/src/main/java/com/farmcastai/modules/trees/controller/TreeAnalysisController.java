package com.farmcastai.modules.trees.controller;

import com.farmcastai.modules.trees.dto.TreeAnalysisRequestDto;
import com.farmcastai.modules.trees.service.TreeAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/v1/trees")
@RequiredArgsConstructor
public class TreeAnalysisController {
    private final TreeAnalysisService treeAnalysisService;

    @PostMapping("/analyze")
    public JsonNode analyze(
            @RequestPart("image") MultipartFile image,
            @RequestParam(required = false) String farmerId,
            @RequestParam(required = false) String county,
            @RequestParam(required = false) Double landAcres,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String notes
    ) {

        TreeAnalysisRequestDto treeAnalysisRequestDto = TreeAnalysisRequestDto.builder()
                .landAcres(landAcres)
                .location(location)
                .notes(notes)
                .county(county)
                .farmerId(farmerId)
                .build();

        return treeAnalysisService.analyze(
                image,
                treeAnalysisRequestDto
        );
    }

    @GetMapping("/history")
    public JsonNode getHistory(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) String cursor
    ) {
        return treeAnalysisService.getHistory(limit, cursor);
    }

    @GetMapping("/quota")
    public JsonNode getQuota() {
        return treeAnalysisService.getQuota();
    }
}
