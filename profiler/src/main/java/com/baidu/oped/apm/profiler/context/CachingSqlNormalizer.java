package com.baidu.oped.apm.profiler.context;

import com.baidu.oped.apm.common.util.ParsingResult;

/**
 * @author emeroad
 */
public interface CachingSqlNormalizer {
    ParsingResult wrapSql(String sql);

    boolean normalizedSql(ParsingResult sql);
}

