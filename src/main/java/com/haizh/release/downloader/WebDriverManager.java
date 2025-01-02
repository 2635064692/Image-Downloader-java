package com.haizh.release.downloader;

import com.haizh.release.downloader.common.ImageParseResult;
import com.haizh.release.downloader.common.ImageResponse;
import com.haizh.release.downloader.engine.SearchEngines;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 浏览器驱动配置类
 */
public class WebDriverManager {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final int CONNECT_TIMEOUT = 3000;
    /**
     * 查询url请求基础driver
     */
    private final WebDriver driver;

    /**
     * 用于可能需要的并发解析网站
     */
    private List<WebDriver> multipleDriver = new ArrayList<>();

    private final Config config;

    public WebDriverManager(Config config) {
        this.config = config;
        this.driver = initializeDriver();
    }

    public WebDriver get(SearchEngines engines) {
        try {
            String url = engines.buildSearchUrl(config.getKeyword());
            driver.get(url);
        } catch (Exception e) {
            System.out.println("webdriver get error : "+e.getMessage());
        }
        return driver;
    }

    public WebDriver initializeDriver() {
        System.setProperty("webdriver.chrome.driver", config.getBrowserDriverPath());
        ChromeOptions options = new ChromeOptions();
        // 设置调试端口
//        options.addArguments("--remote-debugging-port=9222");
        options.addArguments("--headless", "--no-sandbox", "--disable-dev-shm-usage");
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-infobars"); // 禁用“Chrome正在受到自动化测试软件的控制”信息条
        options.addArguments("--disable-extensions"); // 禁用扩展
        options.addArguments("--incognito"); // 启动无痕模式
        options.addArguments("--disable-popup-blocking"); // 禁用弹窗拦截
        if (config.getProxy() != null) {
            options.addArguments("--proxy-server=" + config.getProxy());
        }

        ChromeDriver chromeDriver = new ChromeDriver(options);
//        if (config.getPageLoadTimeout() != null) {
//            chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(5));
//        }

        return chromeDriver;
    }

    public int getLimit() {
        return config.getLimit();
    }

    public WebDriver getDriver() {
        return driver;
    }

    public void quit() {
        this.driver.quit();
    }

    public Set<String> downloadImage(ImageParseResult parseResult, int index) {
        Set<String> newDownloadUrls = new HashSet<>();
        String download_url = parseResult.getImgUrl();
        ImageResponse imageResponse;
        try {

            imageResponse = imageNetRequest(download_url, null);
            imageResponse.setIndex(index);
            save(imageResponse);

        } catch (IOException e) {
            System.out.println("Failed to download image " + index + " from URL: " + download_url + ", error: " + e.getMessage());
            return newDownloadUrls;
        }

        if (parseResult.hasImageRef()) {
            parseRefPageImage(parseResult.getImgRefUrl(), imageResponse.getWidth(), imageResponse.getHeight(), index,null);
        }

        return newDownloadUrls;
    }

    private void save(ImageResponse imageResponse) {
        if (imageResponse.isEmpty()) {
            return;
        }
        String url = imageResponse.getUrl();

        String ext = url.substring(url.lastIndexOf(".")).split("\\?")[0];
        if (!ext.matches("\\.(jpg|jpeg|png|bmp)")) {
            ext = ".jpg";
        }

        int[] combineIndexes = imageResponse.split();
        File outputFile;
        if (combineIndexes[1] == 0) {
            outputFile = new File(config.fetchOutputDirectory(), String.format("image_%04d%s", combineIndexes[0], ext));
        } else {
            outputFile = new File(config.fetchOutputDirectory(), String.format("image_%04d_%02d%s", combineIndexes[0], combineIndexes[1], ext));
        }
        BufferedImage bufferedImage = imageResponse.getIn();

        try {
            ImageIO.write(bufferedImage, ext.replace(".", ""), outputFile);
        } catch (IOException e) {
            System.out.println("Failed to save image " +imageResponse.getUrl());
        }
    }

    private void parseRefPageImage(String imgRefUrl, int baseWidth, int baseHeight, int index,String proxy) {
        if (baseWidth == 0 || baseHeight == 0) {
            return;
        }
        Set<String> tmpUrls = new HashSet<>();
        try {
            // 解析目标网页
            Document document;
            if (proxy != null) {
                String[] proxySplit = proxy.split(":");
                Proxy proxyCls = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxySplit[0], Integer.parseInt(proxySplit[1])));
                document = Jsoup.connect(imgRefUrl).proxy(proxyCls).get();
            }else {
                document = Jsoup.connect(imgRefUrl).get();
            }

            // 获取所有 <img> 标签的图片地址
            Elements images = document.select("img");
            for (Element img : images) {
                String imgUrl = img.absUrl("src"); // 获取图片的绝对 URL

                if (!imgUrl.isEmpty()) {
                    if (imgUrl.startsWith("//")) {
                        imgUrl = "https:" + imgUrl;
                    }
                    tmpUrls.add(imgUrl);
                }
            }

        } catch (IOException e) {
            if (proxy == null && this.config.hasProxy()) {
                System.err.println("parse_ref_page_image Retrying with new proxy...");
                parseRefPageImage(imgRefUrl, baseWidth, baseHeight, index, this.config.getProxy());
            }
            System.out.println("parse ref image error: "+e.getMessage());
        }

        int i = 1;
        for (String tmpUrl : tmpUrls) {
            try {
                String ext = tmpUrl.substring(tmpUrl.lastIndexOf(".")).split("\\?")[0];
                if (".svg".equals(ext) || ".gif".equals(ext)) {
                    continue;
                }
                // 获取当前图片的宽和高
                ImageResponse imageResponse = imageNetRequest(tmpUrl, null);

                // 判断宽或高是否大于给定图片
                if (imageResponse.isSave(baseWidth, baseHeight)) {

                    imageResponse.updateIndex( i,index);
                    // 下载图片
                    save(imageResponse);
                }
                i++;
            }catch (Exception ignored) {
            }

        }
    }

    public static String getFullHostFromUrl(String url) {
        // 查找协议的结束位置
        int protocolEndIndex = 0;
        if (url.contains("://")) {
            protocolEndIndex = url.indexOf("://") + 3;  // 协议部分结束位置
        }

        // 查找路径的开始位置
        int hostEndIndex = url.indexOf('/', protocolEndIndex);  // 从协议结束后开始查找

        if (hostEndIndex == -1) {  // 如果没有找到路径，主机到结尾
            hostEndIndex = url.length();
        }

        // 提取并返回完整的主机部分
        return url.substring(0, hostEndIndex);  // 返回包含协议和主机部分
    }

    private ImageResponse imageNetRequest(String imageUrl, String proxy) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(CONNECT_TIMEOUT);
        if (proxy != null) {
            connection.setRequestProperty("Proxy-Connection", proxy);
        }

        // 确保连接成功
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 400) {
            // 处理输入流
            try (InputStream in = connection.getInputStream()) {
                // 使用 ImageIO 从输入流解析图片
                BufferedImage image = ImageIO.read(in);

                if (image != null) {
                    // 获取图片宽度和高度
                    int width = image.getWidth();
                    int height = image.getHeight();
                    return new ImageResponse(imageUrl, image, height, width);
                } else {
                    System.err.println("Failed to parse image from URL: " + imageUrl);
                    return ImageResponse.empty();
                }
            }
        } else {
            if (proxy == null && this.config.hasProxy()) {
                System.err.println("imageNetRequest Retrying with new proxy...");
                return imageNetRequest(imageUrl, this.config.getProxy());

            }
            return ImageResponse.empty();
        }
    }

}
