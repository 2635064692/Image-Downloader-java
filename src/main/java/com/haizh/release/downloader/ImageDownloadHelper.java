package com.haizh.release.downloader;

import com.haizh.release.downloader.common.ImageParseResult;
import com.haizh.release.downloader.engine.GoogleEngine;
import com.haizh.release.downloader.engine.SearchEngines;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ImageDownloadHelper {

    private ImageDownloadHelper() {
    }

    public static void downloadImagesConcurrently(List<ImageParseResult> results, WebDriverManager manager) {
        if (results == null || results.isEmpty()) {
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Future<Set<String>>> futures = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            int index = i + 1;
            ImageParseResult parseResult = results.get(i);
            futures.add(executor.submit(() -> manager.downloadImage(parseResult, index)));
        }

        List<ImageParseResult> totalUrls = new ArrayList<>();
        for (Future<Set<String>> future : futures) {
            try {
                Set<String> urls = future.get();
                totalUrls.addAll(urls.stream().map(item -> new ImageParseResult(item, null)).collect(Collectors.toList()));
            } catch (InterruptedException | ExecutionException e) {
                System.out.println("Error in concurrent download: " + e.getMessage());
            }
        }
        downloadImagesConcurrently(totalUrls, manager);
        executor.shutdown();
    }

    public static void download(Config config) {
        // 1. 初始化查询引擎
        SearchEngines engines = new GoogleEngine();

        List<Config> configs = config.parseConfig();

        for (Config configItem : configs) {
            // 2. 初始化webdriverManager
            WebDriverManager webDriverManager = new WebDriverManager(configItem);

            // 请求url
            WebDriver driver = webDriverManager.get(engines);

            // 解析driver
            List<ImageParseResult> imageParseResults = engines.parseImage(webDriverManager);

            // result 图片下载
            downloadImagesConcurrently(imageParseResults, webDriverManager);

            webDriverManager.quit();
            System.out.println("loop end");
        }
    }
}
