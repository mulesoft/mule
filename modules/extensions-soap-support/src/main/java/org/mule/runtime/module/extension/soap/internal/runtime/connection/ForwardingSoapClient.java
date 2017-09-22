/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.connection;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.soap.api.client.SoapClientConfiguration.builder;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.soap.api.SoapService;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.client.SoapClientConfigurationBuilder;
import org.mule.runtime.soap.api.client.SoapClientFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import java.util.List;
import java.util.Map;
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
  private final MessageDispatcherProvider<MessageDispatcher> dispatcherProvider;
  private final SoapServiceProvider serviceProvider;
  private final List<WebServiceDefinition> webServiceDefinitions;


  ForwardingSoapClient(SoapService service,
                       SoapServiceProvider serviceProvider,
                       MessageDispatcherProvider<MessageDispatcher> dispatcherProvider) {
    this.serviceProvider = serviceProvider;
    this.webServiceDefinitions = serviceProvider.getWebServiceDefinitions();
    this.dispatcherProvider = dispatcherProvider;
    this.clientsCache = CacheBuilder.<WebServiceDefinition, SoapClient>newBuilder()
        .expireAfterAccess(1, MINUTES)
        .removalListener(new ForwardingClientRemovalListener())
        .build(new SoapClientCacheLoader(service));
  }

  public Map<String, String> getCustomHeaders(String id, String operation) {
    Map<String, String> customHeaders = serviceProvider.getCustomHeaders(getWebServiceDefinitionById(id), operation);
    return customHeaders != null ? customHeaders : emptyMap();
  }

  /**
   * Returns a {@link SoapClient} instance connected to the {@link WebServiceDefinition} of the specified {@code id}.
   *
   * @param id the id of the {@link WebServiceDefinition}.
   * @return a {@link SoapClient} instance
   */
  public SoapClient getSoapClient(String id) {
    try {
      return clientsCache.get(getWebServiceDefinitionById(id));
    } catch (ExecutionException e) {
      throw new MuleRuntimeException(createStaticMessage("Error while retrieving soap client id [" + id + "]"), e);
    }
  }

  private WebServiceDefinition getWebServiceDefinitionById(String id) {
    return webServiceDefinitions.stream().filter(ws -> ws.getServiceId().equals(id)).findAny()
        .orElseThrow(() -> new IllegalArgumentException("Could not find a web service definition with id=[" + id + "]"));
  }

  public List<WebServiceDefinition> getAllWebServices() {
    return copyOf(webServiceDefinitions);
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
          .withDispatcher(dispatcherProvider.connect())
          .withPort(definition.getPort())
          .withWsdlLocation(definition.getWsdlUrl().toString());
      if (definition.getAddress() != null) {
        configurationBuilder.withAddress(definition.getAddress().toString());
      }
      serviceProvider.getSecurities().forEach(configurationBuilder::withSecurity);
      SoapClient soapClient = clientFactory.create(configurationBuilder.build());
      soapClient.start();
      return soapClient;
    }
  }

  /**
   * {@link RemovalListener} implementation stop {@link SoapClient}s.
   */
  private class ForwardingClientRemovalListener implements RemovalListener<WebServiceDefinition, SoapClient> {

    @Override
    public void onRemoval(RemovalNotification<WebServiceDefinition, SoapClient> notification) {
      SoapClient client = notification.getValue();
      try {
        if (client != null) {
          client.stop();
        }
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage("A problem occurred while disconnecting client: '%s'", client), e);
      }
    }
  }
}
