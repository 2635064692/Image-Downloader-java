package com.haizh.release.downloader.common;

import java.awt.image.BufferedImage;

public class ImageResponse {
    private String url;
    private BufferedImage in;
    private int height;
    private int width;
    private int index;

    public ImageResponse(String url, BufferedImage in, int height, int width) {
        this.url = url;
        this.in = in;
        this.height = height;
        this.width = width;
        this.index = 0;
    }

    private ImageResponse() {}

    public static ImageResponse empty() {
        return new ImageResponse();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public BufferedImage getIn() {
        return in;
    }

    public void setIn(BufferedImage in) {
        this.in = in;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isSave(int baseWidth, int baseHeight) {
        if (isEmpty()) {
            return false;
        }

        return this.width >= baseWidth && this.height >= baseHeight;
    }

    public boolean isEmpty() {
        return in == null;
    }

    // 合并高位和低位到一个 int
    public int combine(int high, int low) {
        // 确定低位的位数（14 位足够表示 10000 以内的数字）
        int lowBitSize = 14;

        // 检查低位是否在范围内
        if (low >= (1 << lowBitSize)) { // 2^14 = 16384
            throw new IllegalArgumentException("Low part must be less than " + (1 << lowBitSize));
        }

        // 将高位左移 14 位，并与低位合并
        return (high << lowBitSize) | low;
    }

    // 将一个 int 分割成高位和低位
    public int[] split() {
        // 确定低位的位数（14 位）
        int lowBitSize = 14;

        // 提取低位
        int low = index & ((1 << lowBitSize) - 1); // 获取低 14 位
        // 提取高位
        int high = index >>> lowBitSize; // 无符号右移 14 位

        return new int[]{low,high};
    }

    public void updateIndex(int index, int i) {
        this.index = combine(index, i);
    }
}