package com.veldin.downloader;

import com.veldin.Progress;
import com.veldin.ResultEx;
import com.veldin.ResultTry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.veldin.Main.DOWNLOAD_WIKIPEDIA_ROOT;

public final class WikiDumpDownloader {

    public static final String DUMP_BASE_URL =
            "https://dumps.wikimedia.org/";

    private static final Path DOWNLOAD_ROOT =
            DOWNLOAD_WIKIPEDIA_ROOT.resolve("downloads");

    private static final Path HTML_CACHE_ROOT =
            DOWNLOAD_ROOT.resolve("html");

    private static final Path DUMP_ROOT =
            DOWNLOAD_ROOT.resolve("dump");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final Pattern DUMP_DIRECTORY_PATTERN =
            Pattern.compile("href=\"(\\d{8})/\"");

    private static final Pattern FILE_PATTERN =
            Pattern.compile(
                    "href=\"([^\"]*pages-articles-multistream\\.xml\\.bz2)\""
            );

    private WikiDumpDownloader() {
    }

    /*
     * ============================================================
     * DOWNLOAD LATEST
     * ============================================================
     */

    public static WikiDumpRecord downloadLatest(Wiki wiki)
            throws IOException {

        System.out.println();
        System.out.println("============================================================");
        System.out.println("Wiki: " + wiki.dumpName());
        System.out.println("Index: " + wiki.indexUrl());
        System.out.println("============================================================");

        /*
         * ========================================================
         * FIND LATEST DATE
         * ========================================================
         */

        String indexHtml =
                getCachedHtml(
                        wiki.indexUrl(),
                        wiki.directoryName(),
                        "index.html"
                );

        String latestDate =
                findLatestDate(indexHtml);

        System.out.println("Latest dump: " + latestDate);

        /*
         * ========================================================
         * FIND DUMP FILE
         * ========================================================
         */

        String dumpUrl =
                wiki.indexUrl() + latestDate + "/";

        String dumpHtml =
                getCachedHtml(
                        dumpUrl,
                        wiki.directoryName(),
                        latestDate + ".html"
                );

        String filename =
                findArticleDump(dumpHtml);

        if (filename == null) {
            throw new IOException(
                    "Could not find pages-articles-multistream.xml.bz2 in "
                            + dumpUrl
            );
        }

        /*
         * ========================================================
         * URLS
         * ========================================================
         */

        String fileUrl =
                dumpUrl + filename;

        /*
         * ========================================================
         * OUTPUT PATH
         * ========================================================
         */

        Path outputDirectory =
                DUMP_ROOT
                        .resolve(wiki.directoryName())
                        .resolve(latestDate);

        Path outputFile =
                outputDirectory.resolve(filename);

        /*
         * ========================================================
         * CHECK IF ALREADY DOWNLOADED
         * ========================================================
         */

        if (Files.isRegularFile(outputFile)) {

            System.out.println("Already downloaded:");
            System.out.println(outputFile);

        } else {

            /*
             * ====================================================
             * DOWNLOAD
             * ====================================================
             */

            ResultEx<Void> resultCreateDirectory =
                    ResultTry.doTry(
                            () -> {
                                Files.createDirectories(
                                        outputDirectory
                                );
                                return null;
                            }
                    );

            if (resultCreateDirectory.isError()) {
                throw new IOException(
                        "Could not create dump directory",
                        resultCreateDirectory.unwrapError()
                );
            }

            System.out.println("Downloading:");
            System.out.println(fileUrl);

            System.out.println("To:");
            System.out.println(outputFile);

            download(
                    fileUrl,
                    outputFile
            );

            System.out.println("Download complete.");
        }

        /*
         * ========================================================
         * RETURN MODEL
         * ========================================================
         */

        return new WikiDumpRecord(
                wiki,
                latestDate,
                filename,
                dumpUrl,
                fileUrl,
                outputDirectory,
                outputFile
        );
    }

    /*
     * ============================================================
     * FIND LATEST DUMP DATE
     * ============================================================
     */

    private static String findLatestDate(String html) {

        Matcher matcher =
                DUMP_DIRECTORY_PATTERN.matcher(html);

        List<String> dates =
                new ArrayList<>();

        while (matcher.find()) {
            dates.add(matcher.group(1));
        }

        if (dates.isEmpty()) {
            throw new IllegalStateException(
                    "No dated dump directories found."
            );
        }

        return dates.stream()
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    /*
     * ============================================================
     * FIND ARTICLE DUMP
     * ============================================================
     */

    private static String findArticleDump(String html) {

        Matcher matcher =
                FILE_PATTERN.matcher(html);

        while (matcher.find()) {

            String href =
                    matcher.group(1);

            if (href.endsWith(
                    "pages-articles-multistream.xml.bz2"
            )) {

                return Path.of(href)
                        .getFileName()
                        .toString();
            }
        }

        return null;
    }

    /*
     * ============================================================
     * HTML CACHE
     * ============================================================
     */

    private static String getCachedHtml(
            String url,
            String wikiDirectory,
            String filename
    ) throws IOException {

        ResultEx<Path> getCacheFilePath =
                ResultTry.doTry(() -> {

                    LocalDate today =
                            LocalDate.now();

                    Path cacheDirectory =
                            HTML_CACHE_ROOT
                                    .resolve(wikiDirectory)
                                    .resolve(today.toString());

                    Files.createDirectories(
                            cacheDirectory
                    );

                    return cacheDirectory.resolve(filename);
                });

        if (getCacheFilePath.isError()) {
            throw new IOException(
                    "Could not create cache directory",
                    getCacheFilePath.unwrapError()
            );
        }

        Path cacheFile =
                getCacheFilePath.unwrap();

        /*
         * --------------------------------------------------------
         * USE CACHE
         * --------------------------------------------------------
         */

        if (Files.isRegularFile(cacheFile)) {

            System.out.println("Using cached HTML:");
            System.out.println(cacheFile);

            ResultEx<String> resultRead =
                    ResultTry.doTry(
                            () -> Files.readString(
                                    cacheFile,
                                    StandardCharsets.UTF_8
                            )
                    );

            if (resultRead.isError()) {
                throw new IOException(
                        "Could not read cached HTML",
                        resultRead.unwrapError()
                );
            }

            return resultRead.unwrap();
        }

        /*
         * --------------------------------------------------------
         * DOWNLOAD
         * --------------------------------------------------------
         */

        System.out.println("Downloading HTML:");
        System.out.println(url);

        ResultEx<String> resultGet =
                ResultTry.doTryWithExponentialBackoff(
                        () -> get(url),
                        2,
                        500
                );

        if (resultGet.isError()) {
            throw new IOException(
                    "Could not download the HTML",
                    resultGet.unwrapError()
            );
        }

        String html =
                resultGet.unwrap();

        /*
         * --------------------------------------------------------
         * SAVE CACHE
         * --------------------------------------------------------
         */

        ResultEx<Void> resultWrite =
                ResultTry.doTry(
                        () -> {
                            Files.writeString(
                                    cacheFile,
                                    html,
                                    StandardCharsets.UTF_8
                            );
                            return null;
                        }
                );

        if (resultWrite.isError()) {
            throw new IOException(
                    "Could not write cached HTML",
                    resultWrite.unwrapError()
            );
        }

        System.out.println("Cached HTML:");
        System.out.println(cacheFile);

        return html;
    }

    /*
     * ============================================================
     * HTTP GET
     * ============================================================
     */

    private static String get(String url)
            throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofMinutes(2))
                        .header(
                                "User-Agent",
                                "VeldinWikiDumpDownloader/1.0"
                        )
                        .GET()
                        .build();

        HttpResponse<String> response =
                HTTP.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "HTTP " + response.statusCode()
                            + " while requesting "
                            + url
            );
        }

        return response.body();
    }

    /*
     * ============================================================
     * FILE DOWNLOAD
     * ============================================================
     */
    private static void download(
            String url,
            Path destination
    ) throws IOException {

        ResultEx<HttpResponse<InputStream>> resultResponse =
                ResultTry.doTryWithExponentialBackoff(
                        () -> sendDownloadRequest(url),
                        2,
                        500
                );

        if (resultResponse.isError()) {
            throw new IOException(
                    "Could not start download",
                    resultResponse.unwrapError()
            );
        }

        HttpResponse<InputStream> response =
                resultResponse.unwrap();

        if (response.statusCode() != 200) {

            ResultTry.doTry(
                    () -> {
                        response.body().close();
                        return null;
                    }
            );

            throw new IOException(
                    "HTTP " + response.statusCode()
                            + " while downloading "
                            + url
            );
        }

        long contentLength =
                response.headers()
                        .firstValueAsLong("Content-Length")
                        .orElse(-1);

        Path temporary =
                destination.resolveSibling(
                        destination.getFileName()
                                + ".download"
                );

        System.out.println("Temporary file:");
        System.out.println(temporary);

        ResultEx<Void> resultWrite =
                ResultTry.doTry(
                        () -> {

                            try (
                                    InputStream input =
                                            response.body();

                                    OutputStream output =
                                            Files.newOutputStream(
                                                    temporary
                                            )
                            ) {

                                byte[] buffer = new byte[1024 * 1024];

                                Progress progress = null;
                                if (contentLength > 0) {
                                    progress = new Progress(
                                            "Downloading",
                                            contentLength / 1024.0 / 1024.0,
                                            "MB"
                                    );
                                }

                                int read;

                                while ((read = input.read(buffer))
                                        != -1) {

                                    output.write(
                                            buffer,
                                            0,
                                            read
                                    );

                                    if (progress != null) {
                                        progress.next(
                                                read
                                                        / 1024.0
                                                        / 1024.0
                                        );
                                    }
                                }
                            }

                            return null;
                        }
                );

        if (resultWrite.isError()) {

            ResultTry.doTry(
                    () -> {
                        Files.deleteIfExists(temporary);
                        return null;
                    }
            );

            throw new IOException(
                    "Could not write downloaded file",
                    resultWrite.unwrapError()
            );
        }

        ResultEx<Void> resultMove =
                ResultTry.doTry(
                        () -> {
                            Files.move(
                                    temporary,
                                    destination,
                                    StandardCopyOption.REPLACE_EXISTING
                            );

                            return null;
                        }
                );

        if (resultMove.isError()) {

            ResultTry.doTry(
                    () -> {
                        Files.deleteIfExists(temporary);
                        return null;
                    }
            );

            throw new IOException(
                    "Could not move downloaded file",
                    resultMove.unwrapError()
            );
        }
    }

    private static HttpResponse<InputStream> sendDownloadRequest(
            String url
    ) throws IOException, InterruptedException {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofHours(6))
                        .header(
                                "User-Agent",
                                "VeldinWikiDumpDownloader/1.0"
                        )
                        .GET()
                        .build();

        return HTTP.send(
                request,
                HttpResponse.BodyHandlers.ofInputStream()
        );
    }
}