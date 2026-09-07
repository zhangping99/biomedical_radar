package dev.zhangping.biomedicalradar.collector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FixtureFetchClient implements FetchClient {
    private final Path fixtureDirectory;

    public FixtureFetchClient(Path fixtureDirectory) {
        this.fixtureDirectory = fixtureDirectory.toAbsolutePath().normalize();
    }

    @Override
    public String fetch(SourceDefinition source) throws IOException {
        if (source.fixture() == null || source.fixture().isBlank()) {
            throw new IOException("FIXTURE_NOT_CONFIGURED");
        }
        Path file = fixtureDirectory.resolve(source.fixture()).normalize();
        if (!file.startsWith(fixtureDirectory)) {
            throw new IOException("FIXTURE_PATH_OUTSIDE_ROOT");
        }
        return Files.readString(file, StandardCharsets.UTF_8);
    }
}
