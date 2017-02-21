/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.client;

import static org.mule.services.http.impl.service.client.HttpMessageLogger.LoggerType.REQUESTER;
import org.mule.runtime.api.exception.MuleRuntimeException;

import com.ning.http.client.providers.grizzly.TransportCustomizer;

import org.glassfish.grizzly.filterchain.Filter;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.HttpCodecFilter;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport customizer that adds a probe for logging HTTP messages.
 */
public class LoggerTransportCustomizer implements TransportCustomizer {

  private static final Logger logger = LoggerFactory.getLogger(LoggerTransportCustomizer.class);

  @Override
  public void customize(TCPNIOTransport transport, FilterChainBuilder filterChainBuilder) {
    HttpCodecFilter httpCodecFilter = findHttpCodecFilter(filterChainBuilder);
    httpCodecFilter.getMonitoringConfig().addProbes(new HttpMessageLogger(REQUESTER));
  }

  private HttpCodecFilter findHttpCodecFilter(FilterChainBuilder filterChainBuilder) {
    HttpCodecFilter httpCodecFilter = null;
    try {
      int i = 0;
      do {
        Filter filter = filterChainBuilder.get(i);
        if (filter instanceof HttpCodecFilter) {
          httpCodecFilter = (HttpCodecFilter) filter;
        }
        i++;
      } while (httpCodecFilter == null);
    } catch (IndexOutOfBoundsException e) {
      logger.error(String.format("Failure looking for %s in grizzly client transport", HttpCodecFilter.class.getName()));
      throw new MuleRuntimeException(e);
    }
    return httpCodecFilter;
  }
}
