/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server.grizzly;

import static org.mule.service.http.api.HttpConstants.Protocols.HTTPS;

import java.io.IOException;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.ssl.SSLConnectionContext;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.grizzly.ssl.SSLFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom SSL filter that configures additional properties in the grizzly context.
 */
public class MuleSslFilter extends SSLFilter {

  public static final String SSL_SESSION_ATTRIBUTE_KEY = "muleSslSession";
  private static final Logger logger = LoggerFactory.getLogger(MuleSslFilter.class);

  public MuleSslFilter(SSLEngineConfigurator serverSSLEngineConfigurator, SSLEngineConfigurator clientSSLEngineConfigurator) {
    super(serverSSLEngineConfigurator, clientSSLEngineConfigurator);
  }

  @Override
  public NextAction handleRead(FilterChainContext ctx) throws IOException {
    try {
      ctx.getAttributes().setAttribute(HTTPS.getScheme(), true);
      NextAction nextAction = super.handleRead(ctx);
      ctx.getAttributes().setAttribute(SSL_SESSION_ATTRIBUTE_KEY, getSslSession(ctx));
      return nextAction;
    } catch (SSLHandshakeException e) {
      logger.error("SSL handshake error: " + e.getMessage());
      throw e;
    }
  }

  private SSLSession getSslSession(FilterChainContext ctx) throws SSLPeerUnverifiedException {
    SSLConnectionContext sslConnectionContext = obtainSslConnectionContext(ctx.getConnection());
    if (sslConnectionContext == null) {
      return null;
    }
    return sslConnectionContext.getSslEngine().getSession();
  }

}
