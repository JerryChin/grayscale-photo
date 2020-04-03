package me.jerrychin.grayavatar.controller;

import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * @author Jerry Chin
 */
@Log4j2
@RestController
public class GrayController {
    private File avatarDir;

    GrayController() {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

        avatarDir = new File(tempDir, "avatar");

        if (!avatarDir.exists()) {
            avatarDir.mkdirs();
        }

        log.info("avatar dir: {}", avatarDir.getAbsolutePath());

    }

    @GetMapping("/download")
    public @ResponseBody ResponseEntity<Resource> handleDownload(@RequestParam String fileId) throws IOException {

        if (fileId.contains("..")) {
            log.error("bad file id: {}", fileId);
            throw new SecurityException("bad fileId!");
        }

        File file = new File(avatarDir, fileId);

        log.info("file: {}", file.getAbsolutePath());
        try {
            Path path = Paths.get(file.getAbsolutePath());
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
            return ResponseEntity.ok()
                    .contentLength(file.length())
                    .contentType(MediaType.parseMediaType(URLConnection.guessContentTypeFromName(file.getName())))
                    .body(resource);
        } finally {
            file.delete();
        }
    }

    @Data
    private static class Response {
        private String fileId;
    }

    @PostMapping("/convert")
    public @ResponseBody Response handleGray(@RequestParam("file") MultipartFile file) throws IOException {

        BufferedImage img = ImageIO.read(file.getInputStream());

        int width = img.getWidth();
        int height = img.getHeight();

        for (int i = 0; i < height; i++) {

            for (int j = 0; j < width; j++) {

                int p = img.getRGB(j, i);

                int a = (p >> 24) & 0xff;
                int r = (p >> 16) & 0xff;
                int g = (p >> 8) & 0xff;
                int b = p & 0xff;

                int avg = (r + g + b)/3;

                //replace RGB value with avg
                p = (a << 24) | (avg << 16) | (avg << 8) | avg;

                img.setRGB(j, i, p);
            }
        }


        String ext = FilenameUtils.getExtension(file.getOriginalFilename());
        ext = ext != null ? ext : "jpg";
        File out = new File(avatarDir, UUID.randomUUID().toString() + "." + ext);
        ImageIO.write(img, ext , out);
        Response response = new Response();
        response.fileId = out.getName();
        return response;
    }
}