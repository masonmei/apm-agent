/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baidu.oped.apm.profiler.modifier.db.cubrid;

import com.baidu.oped.apm.bootstrap.context.DatabaseInfo;
import com.baidu.oped.apm.common.ServiceType;
import com.baidu.oped.apm.profiler.modifier.db.ConnectionStringParser;
import com.baidu.oped.apm.profiler.modifier.db.DefaultDatabaseInfo;
import com.baidu.oped.apm.profiler.modifier.db.JDBCUrlParser;
import com.baidu.oped.apm.profiler.modifier.db.StringMaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author emeroad
 */
public class CubridConnectionStringParser implements ConnectionStringParser {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_PORT = 30000;
    public static final String DEFAULT_USER = "public";
    public static final String DEFAULT_PASSWORD = "";

    private static final String URL_PATTERN = "jdbc:cubrid(-oracle|-mysql)?:([a-zA-Z_0-9\\.-]*):([0-9]*):([^:]+):([^:]*):([^:]*):(\\?[a-zA-Z_0-9]+=[^&=?]+(&[a-zA-Z_0-9]+=[^&=?]+)*)?";
    private static final Pattern PATTERN = Pattern.compile(URL_PATTERN, Pattern.CASE_INSENSITIVE);

    @Override
    public DatabaseInfo parse(String url) {
        if (url == null) {
            return JDBCUrlParser.createUnknownDataBase(ServiceType.CUBRID, ServiceType.CUBRID_EXECUTE_QUERY, null);
        }

        final Matcher matcher = PATTERN.matcher(url);
        if (!matcher.find()) {
            logger.warn("Cubrid connectionString parse fail. url:{}", url);
            return JDBCUrlParser.createUnknownDataBase(ServiceType.CUBRID, ServiceType.CUBRID_EXECUTE_QUERY, url);
        }

        String host = matcher.group(2);
        String portString = matcher.group(3);
        String db = matcher.group(4);
        String user = matcher.group(5);
//        String pass = matcher.group(6);
//        String prop = matcher.group(7);

        int port = DEFAULT_PORT;

//        String resolvedUrl;

        if (host == null || host.length() == 0) {
            host = DEFAULT_HOSTNAME;
        }

        if (portString == null || portString.length() == 0) {
            port = DEFAULT_PORT;
        } else {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                logger.warn("cubrid portString parsing fail. portString:{}, url:{}", portString, url);
            }
        }

        if (user == null) {
            user = DEFAULT_USER;
        }

//        if (pass == null) {
//            pass = DEFAULT_PASSWORD;
//        }

//        resolvedUrl = "jdbc:cubrid:" + host + ":" + port + ":" + db + ":" + user + ":********:";

        StringMaker maker = new StringMaker(url);
        String normalizedUrl = maker.clear().before('?').value();

        List<String> hostList = new ArrayList<String>(1);
        final String hostAndPort = host + ":" + portString;
        hostList.add(hostAndPort);

        // skip alt host

        return new DefaultDatabaseInfo(ServiceType.CUBRID, ServiceType.CUBRID_EXECUTE_QUERY, url, normalizedUrl, hostList, db);
    }


    /*
    private DatabaseInfo parseCubrid(String url) {
        // jdbc:cubrid:10.20.30.40:12345:pinpoint:::
        StringMaker maker = new StringMaker(url);
        maker.after("jdbc:cubrid:");
        // 10.11.12.13:3306 In case of replication driver could have multiple values
        // We have to consider mm db too.
        String host = maker.after("//").before('/').value();
        List<String> hostList = new ArrayList<String>(1);
        hostList.add(host);
        // String port = maker.next().after(':').before('/').value();

        String databaseId = maker.next().afterLast('/').before('?').value();
        String normalizedUrl = maker.clear().before('?').value();

        return new DatabaseInfo(ServiceType.CUBRID, ServiceType.CUBRID_EXECUTE_QUERY, url, normalizedUrl, hostList, databaseId);
    }
    */

}
