package dev.zhangping.biomedicalradar.collector;

import java.io.IOException;
import java.net.URI;

public interface FetchClient {
    String fetch(SourceDefinition source) throws IOException, InterruptedException;

    default String fetch(SourceDefinition source, URI uri) throws IOException, InterruptedException {
        return fetch(source);
    }
}
