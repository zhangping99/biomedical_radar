package dev.zhangping.biomedicalradar.collector;

import java.util.List;

public interface SourceConnector {
    String type();

    List<RawSourceItem> collect(SourceDefinition source, FetchClient fetchClient) throws Exception;
}
