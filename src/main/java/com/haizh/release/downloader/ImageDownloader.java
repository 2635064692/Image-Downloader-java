package com.haizh.release.downloader;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Options;

public class ImageDownloader {
    public static void main(String[] args) {
        CommandLineParser parser = new BasicParser();

        // 定义参数选项
        Options options = new Options();
        options.addOption("dp", "driver_path", true, "browser's driver path");
        options.addOption("k", "keyword", true, "image keyword");
        options.addOption("p", "proxy", true, "request proxy");
        options.addOption("plt", "pageLoadTimeout", true, "page load timeout");
        options.addOption("l", "limit", true, "image limit");
        options.addOption("o", "outputDirectory", true, "output directory");
        options.addOption("kf", "keywordsFromFile", true, "extract list of keywords from a text file");
//        options.addOption("c", "classification", true, "search classification");
//        options.addOption("pre", "prefix", true, "download image prefix");

        Config config = new Config();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("dp")) {
                config.setBrowserDriverPath(cmd.getOptionValue("dp"));
            }
            if (cmd.hasOption("k")) {
                config.setKeyword(cmd.getOptionValue("k"));
            }
            if (cmd.hasOption("p")) {
                config.setProxy(cmd.getOptionValue("p"));
            }
            if (cmd.hasOption("plt")) {
                config.setPageLoadTimeout(Integer.parseInt(cmd.getOptionValue("plt")));
            }
            if (cmd.hasOption("l")) {
                config.setLimit(Integer.parseInt(cmd.getOptionValue("l")));
            }
            if (cmd.hasOption("o")) {
                config.setOutputDirectory(cmd.getOptionValue("o"));
            }
            if (cmd.hasOption("kf")) {
                config.setKeywordsFromFile(cmd.getOptionValue("kf"));
            }

        } catch (ParseException e) {
            System.out.println("Parsing failed.  Reason: " + e.getMessage());
            return;
        }

        System.out.println("starting image download...");
        ImageDownloadHelper.download(config);
    }
}
