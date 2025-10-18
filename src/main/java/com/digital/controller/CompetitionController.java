package com.digital.controller;

import com.digital.common.BaseResponse;
import com.digital.common.ResultUtils;
import com.digital.model.entity.Competition;
import com.digital.service.CompetitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 竞赛信息接口
 */
@RestController
@RequestMapping("/competition")
@Slf4j
public class CompetitionController {

    @Resource
    private CompetitionService competitionService;

    /**
     * 获取最新竞赛TOP10
     *
     * @return 竞赛列表
     */
    @GetMapping("/latest")
    public BaseResponse<List<Competition>> getLatestCompetitions() {
        try {
            log.info("开始获取最新竞赛信息");
            List<Competition> competitions = competitionService.getLatestCompetitions();
            log.info("成功获取 {} 个竞赛信息", competitions.size());
            return ResultUtils.success(competitions);
        } catch (Exception e) {
            log.error("获取竞赛信息失败", e);
            return ResultUtils.error(500, "获取竞赛信息失败: " + e.getMessage());
        }
    }
}
