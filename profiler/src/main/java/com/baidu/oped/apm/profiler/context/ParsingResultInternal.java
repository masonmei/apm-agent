package com.baidu.oped.apm.profiler.context;

import com.baidu.oped.apm.common.util.ParsingResult;

/**
 * @author emeroad
 */
interface ParsingResultInternal extends ParsingResult {


    String getOriginalSql();

    boolean setId(int id);

    void setSql(String sql);

    void setOutput(String output);

}
