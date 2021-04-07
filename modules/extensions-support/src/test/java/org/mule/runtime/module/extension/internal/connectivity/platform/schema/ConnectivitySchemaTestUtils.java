/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

/**
 * Testing utilities for functionality related to {@link ConnectivitySchema}
 *
 * @since 4.4.0
 */
public final class ConnectivitySchemaTestUtils {

  private ConnectivitySchemaTestUtils() {}

  /**
   * Creates a {@link ConnectivitySchema} which resembles the ApiKeyAuthentication schema of the Netsuite SOAP API
   *
   * @return a test {@link ConnectivitySchema}
   */
  public static ConnectivitySchema getNetsuiteTokenAuthenticationSchema() {
    return ConnectivitySchema.builder()
        .gav("com.mulesoft.schemas", "mule-netsuite-connector-token-authentication", "1.0")
        .authenticationType("ApiKeyAuthenticationConnection")
        .system("Netsuite")
        .connectionProviderName("token-authentication")
        .addAsset(new ExchangeAssetDescriptor("com.mulesoft.connectors", "citizen-netsuite-connector", "1.0.0-alpha-005"))
        .connectionClassTerm("connectivity.ApiKeyAuthenticationConnection")
        .withParameter("account", p -> p.setPropertyTerm("connectivity.accountId")
            .setRange("string")
            .mandatory(true))
        .withParameter("consumerKey", p -> p.setPropertyTerm("connectivity.clientId")
            .setRange("string")
            .mandatory(true))
        .withParameter("consumerSecret", p -> p.setPropertyTerm("connectivity.clientSecret")
            .setRange("string")
            .mandatory(true))
        .withParameter("tokenId", p -> p.setPropertyTerm("connectivity.tokenId")
            .setRange("string")
            .mandatory(true))
        .withParameter("tokenSecret", p -> p.setPropertyTerm("connectivity.tokenSecret")
            .setRange("string")
            .mandatory(true))
        .withParameter("endpoint", p -> p.setPropertyTerm("apiContract.endpoint")
            .setRange("string")
            .mandatory(false))
        .withParameter("proxy", p -> p.setPropertyTerm("connectivity.proxyConfiguration")
            .setRange("ProxyConfiguration")
            .mandatory(false))
        .withCustomRange("ProxyConfiguration", node -> node.setClassTerm("connectivity.ProxyConfiguration")
            .addParameter("proxyHost", p -> p.setPropertyTerm("connectivity.host")
                .setRange("string")
                .mandatory(false))
            .addParameter("proxyPort", p -> p.setPropertyTerm("connectivity.port")
                .setRange("number")
                .mandatory(false))
            .addParameter("proxyUsername", p -> p.setPropertyTerm("connectivity.username")
                .setRange("string")
                .mandatory(false))
            .addParameter("proxyPassword", p -> p.setPropertyTerm("connectivity.password")
                .setRange("string")
                .mandatory(false)))
        .build();
  }
}
