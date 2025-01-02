package com.haizh.release.downloader.engine;

import com.haizh.release.downloader.WebDriverManager;
import com.haizh.release.downloader.common.ImageParseResult;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLDecoder;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GoogleEngine implements SearchEngines {

    @Override
    public String buildSearchUrl(String keyword) {
        return "https://www.google.com/search?site=&tbm=isch&source=hp&biw=1873&bih=990&q=" + keyword;
    }

    @Override
    public List<ImageParseResult> parseImage(WebDriverManager manager) {
        int maxImages = manager.getLimit();
        WebDriver driver = manager.getDriver();
        List<ImageParseResult> imageUrls = new ArrayList<>();
        Set<ImageParseResult> seenUrls = new HashSet<>();
        int scrollPauseTime = 1000; // 滚动的间隔时间
        long lastHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

        int imgBoxIndex = 0; // 初始化解析的起始索引
        while (imageUrls.size() < maxImages) {
            // 获取所有图片容器
            List<WebElement> imgDivBoxes = driver.findElements(By.xpath("//h3[contains(@class, 'ob5Hkd')]"));

            // 如果没有更多的图片容器，执行滚动操作以加载更多图片
            if (imgBoxIndex >= imgDivBoxes.size()) {
                ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
                try {
                    TimeUnit.MILLISECONDS.sleep(scrollPauseTime);
                } catch (InterruptedException ignored) {
                }
                long newHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) {
                    break;  // 如果页面高度没有变化，说明没有新的图片加载
                }
                lastHeight = newHeight;
                continue; // 重新获取 imgDivBoxes
            }

            // 从上次解析的索引开始，处理图片容器
            List<WebElement> currentBatch = imgDivBoxes.subList(imgBoxIndex, Math.min(imgBoxIndex + (maxImages - imageUrls.size()), imgDivBoxes.size()));

            // 更新 imgBoxIndex
            imgBoxIndex += currentBatch.size();

            // 提取当前 batch 的图片 URL
            List<ImageParseResult> collectedUrls = fetchImageUrlsBatch(currentBatch, driver);

            // 更新已收集的图片 URL，并去重
            for (ImageParseResult url : collectedUrls) {
                if (!seenUrls.contains(url)) {
                    seenUrls.add(url);
                    imageUrls.add(url);
                }
            }
        }

        return imageUrls.size() > maxImages ? imageUrls.subList(0, maxImages) : imageUrls;
    }

    // 从当前 batch 的图片容器中提取 URL
    private List<ImageParseResult> fetchImageUrlsBatch(List<WebElement> imgDivBoxes, WebDriver driver) {
        List<ImageParseResult> results = new ArrayList<>();
        for (WebElement imgDiv : imgDivBoxes) {
            ImageParseResult parseResult = fetchImageUrls(imgDiv, driver);
            if (parseResult.isNotEmpty()) {
                results.add(parseResult);
            }
        }
        return results;
    }

    private WebElement findSubATag(WebElement imgBox) {

        return imgBox.findElement(By.xpath("./a"));
    }

    private ImageParseResult fetchImageUrls(WebElement imgBox, WebDriver driver) {
        ImageParseResult result = ImageParseResult.init();
        try {
            // 模拟移动鼠标加载JavaScript获取href
            Actions action = new Actions(driver);
            action.moveToElement(imgBox).perform();

            // 显式等待，以确保 href 属性被更新
            new WebDriverWait(driver, Duration.of(4, ChronoUnit.SECONDS)).until(
                    d -> findSubATag(imgBox).getAttribute("href") != null &&
                            !findSubATag(imgBox).getAttribute("href").isEmpty()
            );

            // 查找元素内的第一个 <a> 标签
            WebElement tagA = findSubATag(imgBox);
            if (tagA == null) {
                return result;
            }
            String href = tagA.getAttribute("href");

            // 解析查询参数
            Map<String, String> params = parseQueryParams(href);

            // 获取并解码 imgrefurl 和 imgurl 参数
            String imgRefUrl = URLDecoder.decode(params.get("imgrefurl"), "UTF-8");
            String imgUrl = URLDecoder.decode(params.get("imgurl"), "UTF-8");
            result.update(imgUrl, imgRefUrl);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return result;
    }

    // 解析查询参数的方法
    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            } else {
                params.put(keyValue[0], "");
            }
        }
        return params;
    }


    @Override
    public List<ImageParseResult> parseImage(WebDriver manager) {
        return Collections.emptyList();
    }
}
