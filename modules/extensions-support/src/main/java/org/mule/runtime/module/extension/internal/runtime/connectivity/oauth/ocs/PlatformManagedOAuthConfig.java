/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.ocs;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.mule.runtime.api.util.MultiMap.emptyMultiMap;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_CLIENT_SECRET;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ORG_ID;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_URL;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_PLATFORM_AUTH_URL_PATH;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_SERVICE_URL;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.PlatformManagedOAuthGrantType;
import org.mule.runtime.module.extension.internal.runtime.connectivity.oauth.OAuthConfig;

import java.nio.charset.Charset;

/**
 * {@link OAuthConfig} implementation for the {@link PlatformManagedOAuthGrantType}
 *
 * @since 4.3.0
 */
public class PlatformManagedOAuthConfig extends OAuthConfig<PlatformManagedOAuthGrantType> {

  private static final String DEFAULT_PLATFORM_AUTH_URL_SUFFIX = "/oauth2/token";

  private final String connectionUri;
  private final String serviceUrl;
  private final String platformAuthUrl;
  private final String clientId;
  private final String clientSecret;
  private final String orgId;
  private final Charset encoding;
  private final PlatformManagedOAuthGrantType grantType;
  private final ExtensionModel extensionModel;
  private final ConnectionProviderModel delegateConnectionProviderModel;
  private final OAuthGrantType delegateGrantType;

  public static PlatformManagedOAuthConfig from(String ownerConfigName,
                                                String connectionUri,
                                                PlatformManagedOAuthGrantType grantType,
                                                ExtensionModel extensionModel,
                                                ConnectionProviderModel delegateConnectionProviderModel,
                                                OAuthGrantType delegateGrantType,
                                                ConfigurationProperties configurationProperties) {
    return new PlatformManagedOAuthConfig(ownerConfigName,
                                          connectionUri,
                                          getProperty(configurationProperties, OCS_SERVICE_URL),
                                          resolvePlatformAuthUrl(getProperty(configurationProperties, OCS_PLATFORM_AUTH_URL),
                                                                 getProperty(configurationProperties,
                                                                             OCS_PLATFORM_AUTH_URL_PATH,
                                                                             DEFAULT_PLATFORM_AUTH_URL_SUFFIX)),
                                          getProperty(configurationProperties, OCS_CLIENT_ID),
                                          getProperty(configurationProperties, OCS_CLIENT_SECRET),
                                          getProperty(configurationProperties, OCS_ORG_ID),
                                          UTF_8,
                                          grantType,
                                          extensionModel,
                                          delegateConnectionProviderModel,
                                          delegateGrantType);
  }

  private static String getProperty(ConfigurationProperties configurationProperties, String key) {
    return configurationProperties.resolveStringProperty(key)
        .orElseThrow(() -> new IllegalArgumentException(format("OCS property '%s' has not been set", key)));
  }

  private static String getProperty(ConfigurationProperties configurationProperties, String key, String def) {
    return configurationProperties.resolveStringProperty(key).orElse(def);
  }

  private static String resolvePlatformAuthUrl(String url, String suffix) {
    return sanitizeUrl(url) + suffix;
  }

  public PlatformManagedOAuthConfig(String ownerConfigName,
                                    String connectionUri,
                                    String serviceUrl,
                                    String platformAuthUrl,
                                    String clientId,
                                    String clientSecret,
                                    String orgId,
                                    Charset encoding,
                                    PlatformManagedOAuthGrantType grantType,
                                    ExtensionModel extensionModel,
                                    ConnectionProviderModel delegateConnectionProviderModel,
                                    OAuthGrantType delegateGrantType) {
    super(ownerConfigName, empty(), emptyMultiMap(), emptyMultiMap(), emptyMap());
    this.connectionUri = connectionUri;
    this.serviceUrl = serviceUrl;
    this.platformAuthUrl = platformAuthUrl;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.orgId = orgId;
    this.encoding = encoding;
    this.grantType = grantType;
    this.extensionModel = extensionModel;
    this.delegateConnectionProviderModel = delegateConnectionProviderModel;
    this.delegateGrantType = delegateGrantType;
  }

  public String getConnectionUri() {
    return connectionUri;
  }

  public String getServiceUrl() {
    return serviceUrl;
  }

  public String getPlatformAuthUrl() {
    return platformAuthUrl;
  }

  public String getClientId() {
    return clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public String getOrgId() {
    return orgId;
  }

  public Charset getEncoding() {
    return encoding;
  }

  public ConnectionProviderModel getDelegateConnectionProviderModel() {
    return delegateConnectionProviderModel;
  }

  public OAuthGrantType getDelegateGrantType() {
    return delegateGrantType;
  }

  @Override
  public PlatformManagedOAuthGrantType getGrantType() {
    return grantType;
  }


  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  private static String sanitizeUrl(String url) {
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }

    return url;
  }

}
