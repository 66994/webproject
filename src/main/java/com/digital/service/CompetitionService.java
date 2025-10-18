package com.digital.service;

import com.digital.model.entity.Competition;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 竞赛信息解析服务
 */
@Service
@Slf4j
public class CompetitionService {

    private final OkHttpClient httpClient;
    private static final String SAIKR_URL = "https://www.saikr.com/vs";

    public CompetitionService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 获取赛氪网最新竞赛TOP10
     *
     * @return 竞赛列表
     */
    public List<Competition> getLatestCompetitions() {
        try {
            // 从赛氪网获取HTML内容
            String html = fetchHtmlFromSaikr();
            if (html == null || html.isEmpty()) {
                log.error("获取赛氪网HTML内容失败");
                return new ArrayList<>();
            }

            // 解析HTML并提取竞赛信息
            return parseCompetitionsFromHtml(html);
        } catch (Exception e) {
            log.error("获取竞赛信息失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 从赛氪网获取HTML内容
     *
     * @return HTML字符串
     */
    private String fetchHtmlFromSaikr() {
        try {
            Request request = new Request.Builder()
                    .url(SAIKR_URL)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    log.error("HTTP请求失败，状态码: {}", response.code());
                    return null;
                }
            }
        } catch (IOException e) {
            log.error("获取赛氪网HTML内容时发生IO异常", e);
            return null;
        }
    }

    /**
     * 从HTML中解析竞赛信息
     *
     * @param html HTML内容
     * @return 竞赛列表
     */
    private List<Competition> parseCompetitionsFromHtml(String html) {
        List<Competition> competitions = new ArrayList<>();

        try {
            // 解析HTML文档
            Document doc = Jsoup.parse(html);

            // 查找最新竞赛TOP10的容器
            Element listContainer = doc.selectFirst("div.ranking-new-list");
            if (listContainer == null) {
                log.warn("未找到最新竞赛列表容器 div.ranking-new-list");
                return competitions;
            }

            // 提取所有竞赛链接
            Elements competitionLinks = listContainer.select("a");
            log.info("找到 {} 个竞赛链接", competitionLinks.size());

            // 遍历每个竞赛链接，提取信息
            for (Element link : competitionLinks) {
                try {
                    Competition competition = parseCompetitionFromLink(link);
                    if (competition != null) {
                        competitions.add(competition);
                    }
                } catch (Exception e) {
                    log.warn("解析单个竞赛信息失败", e);
                }
            }

            log.info("成功解析 {} 个竞赛信息", competitions.size());
        } catch (Exception e) {
            log.error("解析HTML时发生异常", e);
        }

        return competitions;
    }

    /**
     * 从单个链接元素解析竞赛信息
     *
     * @param link 链接元素
     * @return 竞赛信息
     */
    private Competition parseCompetitionFromLink(Element link) {
        try {
            // 提取链接
            String url = link.attr("href");
            if (url.isEmpty()) {
                return null;
            }

            // 提取排名、名称、热度值（<a>标签下的3个<span>）
            Elements spans = link.select("span");
            if (spans.size() < 3) {
                log.warn("竞赛链接格式异常，span数量: {}", spans.size());
                return null;
            }

            // 解析排名
            String rankText = spans.get(0).text().trim();
            Integer rank = Integer.parseInt(rankText);

            // 解析名称
            String name = spans.get(1).text().trim();

            // 解析热度值
            String popularityText = spans.get(2).text().trim();

            return new Competition(rank, name, popularityText, url);
        } catch (NumberFormatException e) {
            log.warn("解析竞赛数据时数字格式异常", e);
            return null;
        } catch (Exception e) {
            log.warn("解析竞赛信息时发生异常", e);
            return null;
        }
    }
}
