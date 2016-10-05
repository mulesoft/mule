/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.mysql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Utility class for MySQL Connection
 *
 * @since 4.0
 */
final class MySqlDbUtils {

  static String getEffectiveUrl(String urlPrefix, String host, Integer port, String database,
                                Map<String, String> connectionProperties) {
    String url = buildUrlFromAttributes(urlPrefix, host, port, database);
    return addProperties(url, connectionProperties);
  }

  static String getEffectiveUrl(String url, Map<String, String> connectionProperties) {
    return addProperties(url, connectionProperties);
  }

  private static String buildUrlFromAttributes(String urlPrefix, String host, Integer port, String database) {
    String url;
    StringBuilder buf = new StringBuilder(128);
    buf.append(urlPrefix);
    buf.append(host);
    if (port != null && port > 0) {
      buf.append(":");
      buf.append(port);
    }
    buf.append("/");
    if (database != null) {
      buf.append(database);
    }
    url = buf.toString();
    return url;
  }

  private static String addProperties(String url, Map<String, String> connectionProperties) {
    if (connectionProperties != null && !connectionProperties.isEmpty()) {
      final StringBuilder effectiveUrl = new StringBuilder(url);
      final String queryParams = buildQueryParams(connectionProperties);

      if (getUri(url).getQuery() == null) {
        effectiveUrl.append("?");
      } else {
        effectiveUrl.append("&");
      }
      effectiveUrl.append(queryParams);

      return effectiveUrl.toString();
    }

    return url;
  }

  private static String buildQueryParams(Map<String, String> connectionProperties) {
    StringBuilder params = new StringBuilder(128);
    if (connectionProperties != null) {
      for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
        if (params.length() > 0) {
          params.append('&');
        }

        params.append(entry.getKey())
            .append('=')
            .append(entry.getValue());
      }
    }

    return params.toString();
  }

  private static URI getUri(String url) {
    try {
      return new URI(url.substring(5));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to parse database config URL", e);
    }
  }
}
