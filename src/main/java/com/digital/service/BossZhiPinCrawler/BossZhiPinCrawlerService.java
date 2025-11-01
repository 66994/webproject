package com.digital.service.BossZhiPinCrawler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.digital.mapper.JobInfoMapper;
import com.digital.model.entity.JobInfo;
import jakarta.annotation.Resource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * BOSS直聘爬虫主类
 *
 * @author digital
 */
@Service
@Slf4j
public class BossZhiPinCrawlerService {
    private Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
    // HttpClient实例
    private HttpClient httpClient;
    // 要爬取的域名限制
    private String domain;
    // Chrome下载器
    private ChromeDownloaderService chromeDownloader;
    // 页面计数器
    private AtomicInteger pageCounter = new AtomicInteger(1);
    // 最大爬取页面数
    private int maxPages;
    // 工作信息列表
    private List<WorkInfService> workInfList = new ArrayList<>();
    // 搜索关键词
    private String query;
    // 城市代码
    private String cityCode;

    @Resource
    private JobInfoMapper jobInfoMapper;

    public BossZhiPinCrawlerService() {
    }

    public BossZhiPinCrawlerService(String startUrl, String query, String cityCode, int maxPages) {
        initialize(startUrl, query, cityCode, maxPages);
    }

    /**
     * 初始化爬虫参数
     *
     * @param startUrl 起始URL
     * @param query 搜索关键词
     * @param cityCode 城市代码
     * @param maxPages 最大爬取页面数
     */
    public void initialize(String startUrl, String query, String cityCode, int maxPages) {
        log.info("初始化爬虫: query={}, cityCode={}, maxPages={}", query, cityCode, maxPages);
        
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        try {
            URI uri = new URI(startUrl);
            this.domain = uri.getHost();
        } catch (URISyntaxException e) {
            log.error("URL解析失败: {}", startUrl, e);
        }

        try {
            this.chromeDownloader = new ChromeDownloaderService();
            log.info("Chrome下载器初始化成功");
        } catch (Exception e) {
            log.error("Chrome下载器初始化失败", e);
            throw new RuntimeException("Chrome下载器初始化失败", e);
        }
        
        this.maxPages = maxPages;
        this.query = query;
        this.cityCode = cityCode;
        this.visitedUrls.clear();
        this.workInfList.clear();
        this.pageCounter.set(1);
    }

    /**
     * 开始爬取
     * @param startUrl 起始URL
     */
    public void crawl(String startUrl) {
        log.info("开始爬取: {}", startUrl);
        
        if (chromeDownloader == null) {
            log.error("Chrome下载器未初始化，无法开始爬取");
            throw new IllegalStateException("Chrome下载器未初始化，请先调用initialize方法");
        }
        
        Set<String> toVisit = new HashSet<>();
        toVisit.add(startUrl);

        int pagesCrawled = 0;
        while (!toVisit.isEmpty() && pagesCrawled < maxPages) {
            String url = toVisit.iterator().next();
            toVisit.remove(url);

            if (visitedUrls.contains(url)) {
                continue;
            }

            try {
                System.out.println("[爬取进度] 第" + (pagesCrawled + 1) + "页: " + url);
                String htmlContent = fetchHtmlContent(url);
                
                if (htmlContent == null || htmlContent.isEmpty()) {
                    log.warn("HTML内容为空，跳过: {}", url);
                    continue;
                }
                
                visitedUrls.add(url);
                pagesCrawled++;

                // 解析HTML并提取链接
                Set<String> links = extractLinks(htmlContent, url);

                // 处理页面内容
                if (url.contains("/job_detail/")) {
                    // 详情页
                    processDetailPage(htmlContent, url);
                } else {
                    // 列表页
                    processListPage(htmlContent, url);

                    // 添加下一页
                    int nextPage = pageCounter.incrementAndGet();
                    String nextPageUrl = "https://www.zhipin.com/web/geek/job?query=" + query +
                            "&city=" + cityCode + "&page=" + nextPage;
                    if (!visitedUrls.contains(nextPageUrl) && nextPage <= maxPages) {
                        toVisit.add(nextPageUrl);
                    }
                }

                // 将新链接添加到待访问队列
                for (String link : links) {
                    if (!visitedUrls.contains(link) && isSameDomain(link) && link.contains("/job_detail/")) {
                        toVisit.add(link);
                    }
                }

                // 礼貌性延迟，避免请求过于频繁
                Thread.sleep(4000);

            } catch (Exception e) {
                log.error("爬取出错: {}", url, e);
            }
        }

        System.out.println("[爬取完成] 共爬取 " + pagesCrawled + " 页，获取 " + workInfList.size() + " 条职位信息");
        log.info("爬取完成: {}页, {}条职位", pagesCrawled, workInfList.size());
        
        // 关闭浏览器
        try {
            if (chromeDownloader != null) {
                chromeDownloader.close();
            }
        } catch (Exception e) {
            log.error("关闭浏览器出错", e);
        }

        // 保存结果到文件
        saveResultsToFile();
    }

    /**
     * 获取HTML内容
     */
    private String fetchHtmlContent(String url) {
        // 使用ChromeDriver获取页面内容
        return chromeDownloader.download(url);
    }

    /**
     * 从HTML中提取链接
     */
    private Set<String> extractLinks(String html, String baseUrl) {
        Set<String> links = new HashSet<>();
        try {
            Document doc = Jsoup.parse(html, baseUrl);
            Elements linkElements = doc.select("a[href]");

            for (Element link : linkElements) {
                String href = link.attr("abs:href"); // 获取绝对URL
                if (!href.isEmpty() && !href.startsWith("javascript:") && href.contains("zhipin.com")) {
                    links.add(href);
                }
            }
        } catch (Exception e) {
            System.err.println("解析链接时出错: " + e.getMessage());
        }
        return links;
    }

    /**
     * 处理列表页面
     */
    private void processListPage(String html, String url) {
        try {
            Document doc = Jsoup.parse(html);

            // 提取职位链接 - 使用多种选择器确保匹配
            Elements jobLinks = doc.select("a.job-card-left, a[ka*='job_list'], .job-list-box a[href*='/job_detail/']");
            
            int linkCount = 0;
            for (Element link : jobLinks) {
                String jobUrl = link.attr("abs:href");
                if (!jobUrl.isEmpty() && jobUrl.contains("/job_detail/") && !visitedUrls.contains(jobUrl)) {
                    visitedUrls.add(jobUrl);
                    linkCount++;
                }
            }
            
            if (linkCount > 0) {
                System.out.println("列表页提取到 " + linkCount + " 个职位链接");
            }

        } catch (Exception e) {
            log.error("处理列表页时出错: {}", url, e);
        }
    }

    /**
     * 处理详情页面
     */
    private void processDetailPage(String html, String url) {
        try {
            Document doc = Jsoup.parse(html);

            // 检查是否已存在该URL的记录，避免重复存储
            QueryWrapper<JobInfo> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("url", url);
            queryWrapper.eq("isDelete", 0);
            Long count = jobInfoMapper.selectCount(queryWrapper);
            if (count > 0) {
                log.debug("职位信息已存在，跳过: {}", url);
                return;
            }

            JobInfo jobInfo = new JobInfo();
            jobInfo.setUrl(url);

            // 提取工作名称 - 使用多种选择器确保匹配
            Element nameElement = doc.selectFirst("div.name h1, .job-name h1, h1.job-name, .job-detail-header h1");
            if (nameElement != null) {
                jobInfo.setWorkName(nameElement.text().trim());
            } else {
                log.warn("未找到职位名称: {}", url);
            }

            // 提取薪水 - 使用多种选择器
            Element salaryElement = doc.selectFirst("div.name span.salary, .job-primary span.salary, span.salary-text, .salary");
            if (salaryElement != null) {
                jobInfo.setWorkSalary(salaryElement.text().trim());
            }

            // 提取工作地址 - 使用多种选择器
            Element addressElement = doc.selectFirst("div.location-address, .location-address, .job-location, [class*=location]");
            if (addressElement != null) {
                jobInfo.setWorkAddress(addressElement.text().trim());
            }

            // 提取工作内容/职位描述 - 使用多种选择器
            Element contentElement = doc.selectFirst("div.job-sec-text, .job-sec-text, .job-detail-content, .job-detail-text");
            if (contentElement != null) {
                jobInfo.setWorkContent(contentElement.text().trim());
            }

            // 提取工作经验要求 - 使用多种选择器
            Element yearElement = doc.selectFirst("p.text-experience, .text-experience, [class*=experience], .job-require span");
            if (yearElement != null) {
                String yearText = yearElement.text().trim();
                // 尝试从多个地方提取经验要求
                if (yearText.isEmpty()) {
                    Elements yearElements = doc.select(".job-primary-info span, .job-detail-header .text");
                    for (Element el : yearElements) {
                        String text = el.text().trim();
                        if (text.contains("经验") || text.contains("年")) {
                            yearText = text;
                            break;
                        }
                    }
                }
                jobInfo.setWorkYear(yearText);
            }

            // 提取学历要求 - 使用多种选择器
            Element graduateElement = doc.selectFirst("p.text-degree, .text-degree, [class*=degree], .job-require span");
            if (graduateElement != null) {
                String degreeText = graduateElement.text().trim();
                // 尝试从多个地方提取学历要求
                if (degreeText.isEmpty()) {
                    Elements degreeElements = doc.select(".job-primary-info span, .job-detail-header .text");
                    for (Element el : degreeElements) {
                        String text = el.text().trim();
                        if (text.contains("学历") || text.contains("大专") || text.contains("本科") || text.contains("硕士")) {
                            degreeText = text;
                            break;
                        }
                    }
                }
                jobInfo.setGraduate(degreeText);
            }

            // 提取HR活跃时间
            Element hrTimeElement = doc.selectFirst("h2.name span, .hr-info span, .hr-active-time, [class*=active]");
            if (hrTimeElement != null) {
                jobInfo.setHrTime(hrTimeElement.text().trim());
            }

            // 提取公司名 - 使用多种选择器
            Element companyElement = doc.selectFirst("a.company-name, .company-name, .company-info a, [class*=company] a");
            if (companyElement != null) {
                jobInfo.setCompanyName(companyElement.text().trim());
            }
            
            // 验证是否有基本数据（至少要有职位名称）
            if (jobInfo.getWorkName() == null || jobInfo.getWorkName().isEmpty()) {
                log.warn("职位信息不完整，跳过保存: {}", url);
                return;
            }

            // 保存到数据库
            jobInfoMapper.insert(jobInfo);
            
            // 同时添加到列表（用于CSV备份）
            System.out.println("[保存成功] " + (jobInfo.getWorkName() != null ? jobInfo.getWorkName() : "未知职位"));
            WorkInfService workInf = new WorkInfService();
            workInf.setUrl(jobInfo.getUrl());
            workInf.setWorkName(jobInfo.getWorkName());
            workInf.setWorkSalary(jobInfo.getWorkSalary());
            workInf.setWorkAddress(jobInfo.getWorkAddress());
            workInf.setWorkContent(jobInfo.getWorkContent());
            workInf.setWorkYear(jobInfo.getWorkYear());
            workInf.setGraduate(jobInfo.getGraduate());
            workInf.setHRTime(jobInfo.getHrTime());
            workInf.setCompanyName(jobInfo.getCompanyName());
            workInfList.add(workInf);
            
            System.out.println("成功提取并保存工作信息到数据库: " + jobInfo.getWorkName());

        } catch (Exception e) {
            System.err.println("处理详情页时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查URL是否在同一域名下
     */
    private boolean isSameDomain(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost() != null && uri.getHost().equals(domain);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * 保存结果到文件
     */
    private void saveResultsToFile() {
        try (FileWriter writer = new FileWriter("boss_zhipin_results.csv")) {
            // 写入CSV头部（添加工作内容）
            writer.write("URL,工作名称,薪水,工作地址,工作经验,学历要求,公司名称,工作内容\n");

            // 写入数据
            for (WorkInfService workInf : workInfList) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        workInf.getUrl(),
                        workInf.getWorkName() != null ? workInf.getWorkName() : "",
                        workInf.getWorkSalary() != null ? workInf.getWorkSalary() : "",
                        workInf.getWorkAddress() != null ? workInf.getWorkAddress() : "",
                        workInf.getWorkYear() != null ? workInf.getWorkYear() : "",
                        workInf.getGraduate() != null ? workInf.getGraduate() : "",
                        workInf.getCompanyName() != null ? workInf.getCompanyName() : "",
                        workInf.getWorkContent() != null ? workInf.getWorkContent() : ""));
            }

            System.out.println("结果已保存到 boss_zhipin_results.csv，共 " + workInfList.size() + " 条记录");
        } catch (IOException e) {
            System.err.println("保存文件时出错: " + e.getMessage());
        }
    }

    /**
     * 使用示例（注意：作为Spring Service使用时，需要通过Spring容器注入）
     * 在Spring Boot应用中，可以通过Controller或定时任务来调用
     */
    public static void main(String[] args) {
        // 注意：直接运行main方法时，Mapper无法注入，会报错
        // 建议通过Spring Boot应用启动后，通过Controller或定时任务调用
        System.out.println("请通过Spring Boot应用启动后调用此服务，或使用以下代码：");
        System.out.println("// 示例：在Controller中注入并使用");
        System.out.println("// @Resource private BossZhiPinCrawlerService crawlerService;");
        System.out.println("// crawlerService.initialize(startUrl, query, cityCode, maxPages);");
        System.out.println("// crawlerService.crawl(startUrl);");
        
        // 如果需要独立运行（不使用数据库），可以创建不带@Service注解的独立类
        /*
        String query = "Java"; // 搜索关键词
        String cityCode = "101300600"; // 城市代码（广州）
        String startUrl = "https://www.zhipin.com/web/geek/job?query=" + query + "&city=" + cityCode;
        int maxPages = 500; // 最大爬取页面数

        BossZhiPinCrawlerService crawler = new BossZhiPinCrawlerService(startUrl, query, cityCode, maxPages);
        crawler.crawl(startUrl);
        */
    }
}