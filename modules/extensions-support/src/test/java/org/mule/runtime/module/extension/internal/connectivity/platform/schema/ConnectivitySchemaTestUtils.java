/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;

public final class ConnectivitySchemaTestUtils {

  private ConnectivitySchemaTestUtils() {
  }

  public static ConnectivitySchema getNetsuiteTokenAuthenticationSchema() {
    return ConnectivitySchema.builder()
            .setGav("com.mulesoft.schemas", "mule-netsuite-connector-token-authentication", "1.0")
            .addLabel("type", "ApiKeyAuthenticationConnection")
            .addLabel("system", "Netsuite")
            .addLabel("connectionProvider", "token-authentication")
            .addAsset(new BundleDescriptor.Builder()
                    .setGroupId("com.mulesoft.connectors")
                    .setArtifactId("citizen-netsuite-connector")
                    .setVersion("1.0.0-alpha-005")
                    .build())
            .uses("connectivity", "anypoint://semantics/connectivity")
            .usesExternal("core", "anypoint://semantics/core")
            .usesExternal("apiContract", "anypoint://semantics/api-contract")
            .addNode("Connection", node ->
                    node.setClassTerm("connectivity.ApiKeyAuthenticationConnection")
                            .addParameter("account", p -> p.setPropertyTerm("connectivity.accountId")
                                    .setRange("string")
                                    .mandatory(true))
                            .addParameter("consumerKey", p -> p.setPropertyTerm("connectivity.clientId")
                                    .setRange("string")
                                    .mandatory(true))
                            .addParameter("consumerSecret", p -> p.setPropertyTerm("connectivity.clientSecret")
                                    .setRange("string")
                                    .mandatory(true))
                            .addParameter("tokenId", p -> p.setPropertyTerm("connectivity.tokenId")
                                    .setRange("string")
                                    .mandatory(true))
                            .addParameter("tokenSecret", p -> p.setPropertyTerm("connectivity.tokenSecret")
                                    .setRange("string")
                                    .mandatory(true))
                            .addParameter("endpoint", p -> p.setPropertyTerm("apiContract.endpoint")
                                    .setRange("string")
                                    .mandatory(false))
                            .addParameter("proxy", p -> p.setPropertyTerm("connectivity.proxyConfiguration")
                                    .setRange("ProxyConfiguration")
                                    .mandatory(false))
            )
            .addNode("ProxyConfiguration", node -> node.setClassTerm("connectivity.ProxyConfiguration")
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
                            .mandatory(false))
            )
            .build();
  }
}
