package com.example.uploads;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

@Component
@Slf4j
public class UploadHousekeeping {

    private final boolean enabled;
    private final long maxAgeHours;
    private final Path uploadsRoot;

    public UploadHousekeeping(
            @Value("${uploads.cleanup.enabled:true}") boolean enabled,
            @Value("${uploads.cleanup.max-age-hours:72}") long maxAgeHours) {
        this.enabled = enabled;
        this.maxAgeHours = Math.max(maxAgeHours, 1);
        this.uploadsRoot = Path.of(System.getProperty("user.dir"), "uploads");
    }

    @Scheduled(cron = "0 20 * * * *")
    public void sweep() {
        if (!enabled || !Files.exists(uploadsRoot)) {
            return;
        }

        Instant cutoff = Instant.now().minus(maxAgeHours, ChronoUnit.HOURS);
        List<String> volatileDirs = List.of("tmp", "temp", "staging", "temporary");
        for (String dirName : volatileDirs) {
            cleanDirectory(dirName, cutoff);
        }
    }

    private void cleanDirectory(String subdir, Instant cutoff) {
        Path dir = uploadsRoot.resolve(subdir);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(path -> tryDelete(path, cutoff));
        } catch (IOException ex) {
            log.debug("清理上传目录 [{}] 失败: {}", subdir, ex.getMessage());
        }
    }

    private void tryDelete(Path path, Instant cutoff) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            if (lastModified.toInstant().isBefore(cutoff)) {
                Files.deleteIfExists(path);
                log.info("删除过期临时文件: {}", path);
            }
        } catch (IOException ex) {
            log.debug("删除文件 [{}] 失败: {}", path, ex.getMessage());
        }
    }
}
