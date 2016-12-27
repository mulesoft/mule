/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.connection;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.cxf.message.Message.MTOM_ENABLED;
import static org.apache.ws.security.handler.WSHandlerConstants.ACTION;
import static org.apache.ws.security.handler.WSHandlerConstants.PW_CALLBACK_REF;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.INCOMING;
import static org.mule.extension.ws.internal.security.SecurityStrategyType.OUTGOING;
import org.mule.extension.ws.api.SoapVersion;
import org.mule.extension.ws.api.security.SecurityStrategy;
import org.mule.extension.ws.internal.WebServiceConsumer;
import org.mule.extension.ws.internal.interceptor.NamespaceRestorerStaxInterceptor;
import org.mule.extension.ws.internal.interceptor.NamespaceSaverStaxInterceptor;
import org.mule.extension.ws.internal.interceptor.OutputMtomSoapAttachmentsInterceptor;
import org.mule.extension.ws.internal.interceptor.OutputSoapHeadersInterceptor;
import org.mule.extension.ws.internal.interceptor.SoapActionInterceptor;
import org.mule.extension.ws.internal.interceptor.StreamClosingInterceptor;
import org.mule.extension.ws.internal.security.SecurityStrategyType;
import org.mule.extension.ws.internal.security.callback.CompositeCallbackHandler;
import org.mule.extension.ws.internal.transport.HttpDispatcher;
import org.mule.extension.ws.internal.transport.WscDispatcher;
import org.mule.extension.ws.internal.transport.WscTransportFactory;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.service.http.api.HttpService;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.security.auth.callback.CallbackHandler;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap11FaultInInterceptor;
import org.apache.cxf.binding.soap.interceptor.Soap12FaultInInterceptor;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.WrappedOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;

/**
 * Factory class that creates instances of {@link WscClient}s.
 *
 * @since 4.0
 */
final class ClientFactory {

  private ClientFactory() {}

  static ClientFactory getInstance() {
    return new ClientFactory();
  }

  /**
   * Creates a new instance of a {@link WscClient} for the given address ans soap version.
   * <p>
   * Adds all the custom {@link Interceptor}s to work with the {@link WebServiceConsumer}.
   *
   * @throws ConnectionException if the client couldn't be created.
   */
  WscClient create(String address,
                   SoapVersion soapVersion,
                   boolean mtomEnabled,
                   List<SecurityStrategy> securities,
                   HttpService httpService,
                   String transportConfig)
      throws ConnectionException {
    WscTransportFactory factory = new WscTransportFactory();
    Client client = factory.createClient(address, soapVersion.getVersion());
    client.getEndpoint().put(MTOM_ENABLED, mtomEnabled);
    addSecurityInterceptors(client, securities);
    addRequestInterceptors(client);
    addResponseInterceptors(client, mtomEnabled);
    removeUnnecessaryCxfInterceptors(client);
    WscDispatcher dispatcher = createDispatcher(address, httpService, transportConfig);
    return new WscClient(client, dispatcher, soapVersion, mtomEnabled);
  }

  private void addSecurityInterceptors(Client client, List<SecurityStrategy> securityStrategies) {
    Map<String, Object> requestProps = buildSecurityProperties(securityStrategies, OUTGOING);
    if (!requestProps.isEmpty()) {
      client.getOutInterceptors().add(new WSS4JOutInterceptor(requestProps));
    }

    Map<String, Object> responseProps = buildSecurityProperties(securityStrategies, INCOMING);
    if (!responseProps.isEmpty()) {
      client.getInInterceptors().add(new WSS4JInInterceptor(responseProps));
    }
  }

  private Map<String, Object> buildSecurityProperties(List<SecurityStrategy> strategies, SecurityStrategyType type) {

    if (strategies.isEmpty()) {
      return emptyMap();
    }

    Map<String, Object> props = new HashMap<>();
    StringJoiner actionsJoiner = new StringJoiner(" ");

    ImmutableList.Builder<CallbackHandler> callbackHandlersBuilder = ImmutableList.builder();
    strategies.stream()
        .filter(s -> s.securityType().equals(type))
        .forEach(s -> {
          props.putAll(s.buildSecurityProperties());
          actionsJoiner.add(s.securityAction());
          s.buildPasswordCallbackHandler().ifPresent(callbackHandlersBuilder::add);
        });

    List<CallbackHandler> handlers = callbackHandlersBuilder.build();
    if (!handlers.isEmpty()) {
      props.put(PW_CALLBACK_REF, new CompositeCallbackHandler(handlers));
    }

    String actions = actionsJoiner.toString();
    if (isNotBlank(actions)) {
      props.put(ACTION, actions);
    }

    // This Map needs to be mutable, cxf will add properties if needed.
    return props;
  }

  private void addRequestInterceptors(Client client) {
    List<Interceptor<? extends Message>> outInterceptors = client.getOutInterceptors();
    outInterceptors.add(new SoapActionInterceptor());
  }

  private void addResponseInterceptors(Client client, boolean mtomEnabled) {
    List<Interceptor<? extends Message>> inInterceptors = client.getInInterceptors();
    inInterceptors.add(new NamespaceRestorerStaxInterceptor());
    inInterceptors.add(new NamespaceSaverStaxInterceptor());
    inInterceptors.add(new StreamClosingInterceptor());
    inInterceptors.add(new CheckFaultInterceptor());
    inInterceptors.add(new OutputSoapHeadersInterceptor());
    inInterceptors.add(new SoapActionInterceptor());
    if (mtomEnabled) {
      inInterceptors.add(new OutputMtomSoapAttachmentsInterceptor());
    }
  }

  private void removeUnnecessaryCxfInterceptors(Client client) {
    Binding binding = client.getEndpoint().getBinding();
    removeInterceptor(binding.getOutInterceptors(), WrappedOutInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap11FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap12FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), CheckFaultInterceptor.class.getName());
  }

  private void removeInterceptor(List<Interceptor<? extends Message>> inInterceptors, String name) {
    inInterceptors.removeIf(i -> i instanceof PhaseInterceptor && ((PhaseInterceptor) i).getId().equals(name));
  }

  private WscDispatcher createDispatcher(String address, HttpService httpService, String transportConfig)
      throws ConnectionException {
    String protocol = address.substring(0, address.indexOf("://"));
    if (transportConfig == null) {
      if (protocol.equals("http")) {
        return HttpDispatcher.createDefault(address, httpService);
      }
      throw new ConnectionException(format("Cannot create a default dispatcher for the [%s] protocol", protocol));
    }
    // TODO: MULE-10783: use custom transport configuration
    throw new UnsupportedOperationException(format("cannot create a dispatcher for config [%s]", transportConfig));
  }
}
