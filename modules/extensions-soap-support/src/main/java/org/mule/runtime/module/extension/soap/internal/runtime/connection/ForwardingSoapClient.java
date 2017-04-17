/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.services.soap.api.client.SoapClientConfiguration.builder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.services.soap.api.SoapService;
import org.mule.services.soap.api.client.SoapClient;
import org.mule.services.soap.api.client.SoapClientConfigurationBuilder;
import org.mule.services.soap.api.client.SoapClientFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This client is just a manager of {@link SoapClient} instances, since a single {@link SoapServiceProvider} can connect with
 * multiple services.
 * <p>
 * This client will create {@link SoapClient} instances lazily and only when required, this way we avoid instantiating all
 * multiple clients for each different {@link ForwardingSoapClient} that is created, optimising resources.
 *
 * @since 4.0
 */
public class ForwardingSoapClient {

  private final LoadingCache<WebServiceDefinition, SoapClient> clientsCache;
  private final SoapServiceProvider serviceProvider;

  ForwardingSoapClient(SoapService service, SoapServiceProvider serviceProvider) {
    this.serviceProvider = serviceProvider;
    this.clientsCache = CacheBuilder.<WebServiceDefinition, SoapClient>newBuilder()
        .expireAfterAccess(1, MINUTES)
        .removalListener(new ForwardingClientRemovalListener())
        .build(new SoapClientCacheLoader(service));
  }

  /**
   * Returns a {@link SoapClient} instance connected to the {@link WebServiceDefinition} of the specified {@code id}.
   *
   * @param id the id of the {@link WebServiceDefinition}.
   * @return a {@link SoapClient} instance
   * @throws MuleException in any error case.
   */
  public SoapClient getSoapClient(String id) throws MuleException {
    List<WebServiceDefinition> webServiceDefinitions = serviceProvider.getWebServiceDefinitions();
    WebServiceDefinition wsd = webServiceDefinitions.stream().filter(ws -> ws.getServiceId().equals(id)).findAny()
        .orElseThrow(() -> new IllegalArgumentException("Could not find a soap client id [" + id + "]"));
    try {
      return clientsCache.get(wsd);
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(createStaticMessage("Error while retrieving soap client id [" + id + "]"), e);
    }
  }

  /**
   * Disconnects all the {@link SoapClient} instances created by this manager.
   */
  public void disconnect() {
    clientsCache.invalidateAll();
  }

  /**
   * {@link CacheLoader} implementation to load lazily {@link SoapClient}s.
   */
  private class SoapClientCacheLoader extends CacheLoader<WebServiceDefinition, SoapClient> {

    private final SoapService service;

    private SoapClientCacheLoader(SoapService service) {
      this.service = service;
    }

    @Override
    public SoapClient load(WebServiceDefinition definition) throws Exception {
      SoapClientFactory clientFactory = service.getClientFactory();
      SoapClientConfigurationBuilder configurationBuilder = builder()
          .withService(definition.getService())
          .withPort(definition.getPort())
          .withWsdlLocation(definition.getWsdlUrl().toString());
      if (definition.getAddress() != null) {
        configurationBuilder.withAddress(definition.getAddress().toString());
      }
      serviceProvider.getSecurities().forEach(configurationBuilder::withSecurity);
      return clientFactory.create(configurationBuilder.build());
    }
  }

  /**
   * {@link RemovalListener} implementation stop {@link SoapClient}s.
   */
  private class ForwardingClientRemovalListener implements RemovalListener<WebServiceDefinition, SoapClient> {

    @Override
    public void onRemoval(RemovalNotification<WebServiceDefinition, SoapClient> notification) {
      try {
        SoapClient client = notification.getValue();
        if (client != null) {
          client.stop();
        }
      } catch (MuleException e) {
        throw new MuleRuntimeException(createStaticMessage("A problem occurred while disconnecting client: '%s'"), e);
      }
    }
  }
}
