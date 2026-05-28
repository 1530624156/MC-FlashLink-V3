package cn.com.mcflashlink.client.service.wintun;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

final class NativeWintunLibraryLoader {
    private NativeWintunLibraryLoader() {
    }

    static String extractLibraryPath() {
        String arch = windowsArch();
        String resourcePath = "/native/windows/" + arch + "/wintun.dll";
        Path directory = cacheDirectory().resolve(arch);
        Path stableTarget = directory.resolve("wintun.dll");

        try (InputStream inputStream = NativeWintunLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new WintunException("未找到内置 Wintun DLL: " + resourcePath);
            }
            Files.createDirectories(directory);
            if (Files.exists(stableTarget) && Files.size(stableTarget) > 0) {
                return stableTarget.toAbsolutePath().toString();
            }
            return copyToAvailableTarget(inputStream, stableTarget, directory, arch).toAbsolutePath().toString();
        } catch (IOException exception) {
            throw new WintunException("释放内置 Wintun DLL 失败：" + exception.getMessage(), exception);
        }
    }

    private static Path copyToAvailableTarget(InputStream inputStream, Path stableTarget, Path directory, String arch) throws IOException {
        try {
            Files.copy(inputStream, stableTarget);
            return stableTarget;
        } catch (FileAlreadyExistsException exception) {
            return stableTarget;
        } catch (IOException exception) {
            Path uniqueTarget = directory.resolve("wintun-" + arch + "-" + UUID.randomUUID().toString() + ".dll");
            try (InputStream retryInputStream = NativeWintunLibraryLoader.class.getResourceAsStream("/native/windows/" + arch + "/wintun.dll")) {
                if (retryInputStream == null) {
                    throw exception;
                }
                Files.copy(retryInputStream, uniqueTarget);
                return uniqueTarget;
            }
        }
    }

    private static Path cacheDirectory() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.trim().isEmpty()) {
            return Paths.get(localAppData, "MC-FlashLink", "native");
        }
        return Paths.get(System.getProperty("user.home"), ".mc-flashlink", "native");
    }

    private static String windowsArch() {
        String arch = System.getProperty("os.arch", "").toLowerCase();
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64";
        }
        if (arch.equals("arm") || arch.startsWith("armv7")) {
            return "arm";
        }
        if (arch.contains("64")) {
            return "amd64";
        }
        return "x86";
    }
}
