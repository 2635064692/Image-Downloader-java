package com.haizh.release.downloader.engine;

import com.haizh.release.downloader.WebDriverManager;
import com.haizh.release.downloader.common.ImageParseResult;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * 搜索引擎抽象
 */
public interface SearchEngines {
    /**
     * 相关搜索引擎查询url构建
     * @param keyword
     * @return
     */
    String buildSearchUrl(String keyword);

    List<ImageParseResult> parseImage(WebDriverManager manager);

    List<ImageParseResult> parseImage(WebDriver manager);
}
