package dev.zhangping.biomedicalradar.collector;

import java.io.IOException;

public interface FetchClient {
    String fetch(SourceDefinition source) throws IOException, InterruptedException;
}
