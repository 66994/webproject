package com.digital.service.BossZhiPinCrawler;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import io.github.bonigarcia.wdm.WebDriverManager;

public class ChromeDownloaderService {
    // 声明驱动
    private RemoteWebDriver driver;


    public ChromeDownloaderService() {
        // 自动下载并设置 chromedriver
        WebDriverManager.chromedriver().setup();

        // 创建浏览器参数对象
        ChromeOptions chromeOptions = new ChromeOptions();
        // chromeOptions.addArguments("--headless");
        chromeOptions.addArguments("--window-size=1280,700");
        chromeOptions.addArguments("--disable-blink-features=AutomationControlled");
        chromeOptions.addArguments("--user-agent=Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.6668.101 Safari/537.36");

        // 创建驱动
        this.driver = new ChromeDriver(chromeOptions);
    }

    public String download(String url) {
        try {
            driver.get(url);
            Thread.sleep(4000);

            // 滚动到页面底部，确保所有内容加载（添加空值检查）
            try {
                Object result = driver.executeScript(
                    "if (document.body && document.body.scrollHeight) { " +
                    "window.scrollTo(0, document.body.scrollHeight - 1000); " +
                    "return true; } else { return false; }"
                );
                if (result != null && result.equals(true)) {
                    Thread.sleep(3000);
                }
            } catch (Exception e) {
                // 如果滚动失败，继续执行，不影响页面内容获取
                System.out.println("页面滚动失败，继续获取内容: " + e.getMessage());
            }

            // 获取页面源代码
            String pageSource = driver.getPageSource();

            return pageSource;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }
}