package com.haizh.release.downloader;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Config {
    private String proxy;

    private String keyword;

    private Integer pageLoadTimeout;

    private String browserDriverPath;

    private int limit = 10;

    private String outputDirectory;

    private String keywordsFromFile;

    private String subFolder = "default";

    public String getSubFolder() {
        return subFolder;
    }

    public void setSubFolder(String subFolder) {
        this.subFolder = subFolder;
    }

    public String getKeywordsFromFile() {
        return keywordsFromFile;
    }

    public void setKeywordsFromFile(String keywordsFromFile) {
        this.keywordsFromFile = keywordsFromFile;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getKeyword() {
        return keyword.replace("&", "%26");
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getPageLoadTimeout() {
        return pageLoadTimeout;
    }

    public void setPageLoadTimeout(Integer pageLoadTimeout) {
        this.pageLoadTimeout = pageLoadTimeout;
    }

    public String getBrowserDriverPath() {
        return browserDriverPath;
    }

    public void setBrowserDriverPath(String browserDriverPath) {
        this.browserDriverPath = browserDriverPath;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public String fetchOutputDirectory() {
        String outputPath = outputDirectory == null ? "./download" : outputDirectory;
        createDirectory(Paths.get(outputPath));

        String subfolder = outputPath + File.separator + subFolder;
        createDirectory(Paths.get(subfolder));

        return subfolder;
    }

    private void createDirectory(Path dirPath) {
        try {
            if (Files.notExists(dirPath)) {
                Files.createDirectories(dirPath);
                System.out.println("Directory created: " + dirPath);
            }
        } catch (IOException e) {
            System.err.println("Failed to create directory: " + dirPath);
            e.printStackTrace();
        }
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    // 解析配置的方法
    public List<Config> parseConfig() {
        if (keywordsFromFile == null) {
            List<Config> result = new ArrayList<>();
            result.add(this);
            return result;
        }

        List<Config> results = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 从 JSON 文件中读取数据
            JSONObject[] items = objectMapper.readValue(new File(keywordsFromFile), JSONObject[].class);
            for (JSONObject item : items) {
                Config copiedConfig = JSONObject.parseObject(JSON.toJSONString(this), Config.class);

                // 深拷贝
                copiedConfig.setKeyword(item.getString("keywords"));
                copiedConfig.setLimit(item.getIntValue("limit"));
                copiedConfig.setSubFolder(item.getString("sub_folder"));

                results.add(copiedConfig);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return results;
    }

    public boolean hasProxy() {
        return !Strings.isNullOrEmpty(proxy);
    }
}
