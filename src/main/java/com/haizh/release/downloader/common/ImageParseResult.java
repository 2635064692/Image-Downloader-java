package com.haizh.release.downloader.common;

public class ImageParseResult {

    /**
     * 图片地址
     */
    private String imgUrl;

    /**
     * 图片来源地址
     */
    private String imgRefUrl;

    public ImageParseResult(String imgUrl, String imgRefUrl) {
        this.imgUrl = imgUrl;
        this.imgRefUrl = imgRefUrl;
    }

    public ImageParseResult() {
    }

    public static ImageParseResult init() {
        return new ImageParseResult();
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getImgRefUrl() {
        return imgRefUrl;
    }

    public boolean hasImageRef() {
        return imgRefUrl != null;
    }

    public void update(String imgUrl, String imgRefUrl) {
        this.imgUrl = imgUrl;
        this.imgRefUrl = imgRefUrl;
    }

    public boolean isNotEmpty() {
        return imgUrl != null && imgRefUrl != null;
    }

    @Override
    public int hashCode() {
        return this.imgRefUrl.hashCode();
    }
}
