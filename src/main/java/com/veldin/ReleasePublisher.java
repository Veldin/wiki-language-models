package com.veldin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReleasePublisher {

    private ReleasePublisher() {
    }

    public static void publish(
            Release release,
            Path releaseDirectory
    ) throws IOException, InterruptedException {

        ensureGhLogin();

        String tag = release.getName();

        // Replace existing local tag.
        run(
                "git",
                "tag",
                "-f",
                tag
        );

        // Replace existing remote tag.
        run(
                "git",
                "push",
                "--force",
                "origin",
                tag
        );

        // Remove existing GitHub release, if present.
        deleteExistingRelease(tag);

        List<String> command = new ArrayList<>();

        command.add("gh");
        command.add("release");
        command.add("create");
        command.add(tag);

        try (var files = Files.list(releaseDirectory)) {
            files
                    .filter(Files::isRegularFile)
                    .forEach(file -> command.add(file.toString()));
        }

        command.add("--title");
        command.add(tag);
        command.add("--notes-file");
        command.add(
                releaseDirectory
                        .resolve("dump-info.yaml")
                        .toString()
        );

        run(command.toArray(String[]::new));
    }

    private static void ensureGhLogin()
            throws IOException, InterruptedException {

        Process process =
                new ProcessBuilder(
                        "gh",
                        "auth",
                        "status"
                )
                        .inheritIO()
                        .start();

        int exitCode = process.waitFor();

        if (exitCode == 0) {
            return;
        }

        System.out.println(
                "GitHub CLI is not logged in. Starting GitHub login..."
        );

        run(
                "gh",
                "auth",
                "login"
        );
    }

    private static void deleteExistingRelease(
            String tag
    ) throws IOException, InterruptedException {

        Process process =
                new ProcessBuilder(
                        "gh",
                        "release",
                        "delete",
                        tag,
                        "--yes"
                )
                        .inheritIO()
                        .start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.out.println(
                    "No existing GitHub release found: " + tag
            );
        }
    }

    private static void run(
            String... command
    ) throws IOException, InterruptedException {

        Process process =
                new ProcessBuilder(command)
                        .inheritIO()
                        .start();

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "Command failed (" + exitCode + "): "
                            + String.join(" ", command)
            );
        }
    }
}