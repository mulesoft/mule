/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection;

import static org.apache.commons.lang.StringUtils.isEmpty;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Base class for a vendor specific connection provider.
 * Connection can be specified through a URL, or through
 * convenience parameters exposed to spare the user from
 * the need of knowing the specific URL format.
 * <p>
 * Notice those parameters are ignored if a specific URL
 * is provided.
 *
 * @since 4.0
 */
public abstract class AbstractVendorConnectionProvider extends DbConnectionProvider {

  /**
   * The name of the database. Must be configured unless a full JDBC URL is configured.
   */
  @Parameter
  @Optional
  private String database;

  /**
   * Configures just the host part of the JDBC URL (and leaves the rest of the default
   * JDBC URL untouched).
   */
  @Parameter
  @Optional
  private String host;

  /**
   * Configures just the port part of the JDBC URL (and leaves the rest of the default
   * JDBC URL untouched).
   */
  @Parameter
  @Optional
  private Integer port;

  /**
   * Specifies a list of custom key-value connectionProperties for the config.
   */
  @Parameter
  @Optional
  private Map<String, String> connectionProperties;

  protected abstract String getUrlPrefix();

  @Override
  protected String getEffectiveUrl() {
    String url = getConnectionParameters().getDataSourceConfig().getUrl();
    if (isEmpty(url)) {
      url = buildUrlFromAttributes();
    }

    return addProperties(url);
  }

  protected String buildUrlFromAttributes() {
    String url;
    StringBuilder buf = new StringBuilder(128);
    buf.append(getUrlPrefix());
    buf.append(getHost());
    if (getPort() != null && getPort() > 0) {
      buf.append(":");
      buf.append(getPort());
    }
    buf.append("/");
    buf.append(getDatabase());
    url = buf.toString();
    return url;
  }

  private String addProperties(String url) {
    Map<String, String> connectionProperties = getConnectionProperties();

    if (connectionProperties.isEmpty()) {
      return url;
    }

    StringBuilder effectiveUrl = new StringBuilder(url);

    if (getUri(url).getQuery() == null) {
      effectiveUrl.append("?");
    } else {
      effectiveUrl.append("&");
    }

    return effectiveUrl.append(buildQueryParams(connectionProperties)).toString();
  }

  private String buildQueryParams(Map<String, String> connectionProperties) {
    StringBuilder params = new StringBuilder(128);
    for (Map.Entry<String, String> entry : connectionProperties.entrySet()) {
      if (params.length() > 0) {
        params.append('&');
      }

      params.append(entry.getKey())
          .append('=')
          .append(entry.getValue());
    }

    return params.toString();
  }

  private URI getUri(String url) {
    try {
      return new URI(url.substring(5));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to parse database config URL", e);
    }
  }

  public String getDatabase() {
    return database;
  }

  public String getHost() {
    return host;
  }

  public Integer getPort() {
    return port;
  }

  public Map<String, String> getConnectionProperties() {
    return connectionProperties;
  }
}
