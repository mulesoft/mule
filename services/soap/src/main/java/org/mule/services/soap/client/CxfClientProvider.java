/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.soap.client;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.cxf.message.Message.MTOM_ENABLED;
import static org.apache.ws.security.handler.WSHandlerConstants.ACTION;
import static org.apache.ws.security.handler.WSHandlerConstants.PW_CALLBACK_REF;
import org.mule.services.soap.api.SoapVersion;
import org.mule.services.soap.api.client.SoapClientConfiguration;
import org.mule.services.soap.api.security.SecurityStrategy;
import org.mule.services.soap.api.security.SecurityStrategyVisitor;
import org.mule.services.soap.api.security.DecryptSecurityStrategy;
import org.mule.services.soap.api.security.EncryptSecurityStrategy;
import org.mule.services.soap.api.security.SignSecurityStrategy;
import org.mule.services.soap.api.security.TimestampSecurityStrategy;
import org.mule.services.soap.api.security.UsernameTokenSecurityStrategy;
import org.mule.services.soap.api.security.VerifySignatureSecurityStrategy;
import org.mule.services.soap.interceptor.NamespaceRestorerStaxInterceptor;
import org.mule.services.soap.interceptor.NamespaceSaverStaxInterceptor;
import org.mule.services.soap.interceptor.OutputMtomSoapAttachmentsInterceptor;
import org.mule.services.soap.interceptor.OutputSoapHeadersInterceptor;
import org.mule.services.soap.interceptor.SoapActionInterceptor;
import org.mule.services.soap.interceptor.StreamClosingInterceptor;
import org.mule.services.soap.security.SecurityStrategyCxfAdapter;
import org.mule.services.soap.security.SecurityStrategyType;
import org.mule.services.soap.security.WssDecryptSecurityStrategyCxfAdapter;
import org.mule.services.soap.security.WssEncryptSecurityStrategyCxfAdapter;
import org.mule.services.soap.security.WssSignSecurityStrategyCxfAdapter;
import org.mule.services.soap.security.WssTimestampSecurityStrategyCxfAdapter;
import org.mule.services.soap.security.WssUsernameTokenSecurityStrategyCxfAdapter;
import org.mule.services.soap.security.WssVerifySignatureSecurityStrategyCxfAdapter;
import org.mule.services.soap.security.callback.CompositeCallbackHandler;
import org.mule.services.soap.transport.SoapServiceTransportFactory;

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
 * Object that creates CXF specific clients based on a {@link SoapClientConfiguration} setting all the required CXF properties.
 * <p>
 * the created client aims to be the CXF client used in the {@link SoapCxfClient}.
 *
 * @since 4.0
 */
public class CxfClientProvider {

  static Client getClient(SoapClientConfiguration configuration) {
    SoapServiceTransportFactory factory = new SoapServiceTransportFactory();
    boolean isMtom = configuration.isMtomEnabled();
    String address = configuration.getAddress();
    SoapVersion version = configuration.getVersion();
    Client client = factory.createClient(address, version.getVersion());
    addSecurityInterceptors(client, getAdaptedSecurities(configuration.getSecurities()));
    addRequestInterceptors(client);
    addResponseInterceptors(client, isMtom);
    client.getEndpoint().put(MTOM_ENABLED, isMtom);
    removeUnnecessaryCxfInterceptors(client);
    return client;
  }

  private static List<SecurityStrategyCxfAdapter> getAdaptedSecurities(List<SecurityStrategy> securities) {
    ImmutableList.Builder<SecurityStrategyCxfAdapter> builder = ImmutableList.builder();
    securities.forEach(s -> s.accept(new SecurityStrategyVisitor() {

      @Override
      public void visitEncrypt(EncryptSecurityStrategy encrypt) {
        builder.add(new WssEncryptSecurityStrategyCxfAdapter(encrypt.getKeyStoreConfiguration()));
      }

      @Override
      public void visitDecrypt(DecryptSecurityStrategy decrypt) {
        builder.add(new WssDecryptSecurityStrategyCxfAdapter(decrypt.getKeyStoreConfiguration()));
      }

      @Override
      public void visitUsernameToken(UsernameTokenSecurityStrategy usernameToken) {
        builder.add(new WssUsernameTokenSecurityStrategyCxfAdapter(usernameToken));
      }

      @Override
      public void visitSign(SignSecurityStrategy sign) {
        builder.add(new WssSignSecurityStrategyCxfAdapter(sign.getKeyStoreConfiguration()));
      }

      @Override
      public void visitVerify(VerifySignatureSecurityStrategy verify) {
        WssVerifySignatureSecurityStrategyCxfAdapter adapter =
            verify.getTrustStoreConfiguration().map(WssVerifySignatureSecurityStrategyCxfAdapter::new)
                .orElse(new WssVerifySignatureSecurityStrategyCxfAdapter());
        builder.add(adapter);
      }

      @Override
      public void visitTimestamp(TimestampSecurityStrategy timestamp) {
        builder.add(new WssTimestampSecurityStrategyCxfAdapter(timestamp.getTimeToLeaveInSeconds()));
      }
    }));

    return builder.build();
  }

  private static void addSecurityInterceptors(Client client, List<SecurityStrategyCxfAdapter> securityStrategies) {
    Map<String, Object> requestProps = buildSecurityProperties(securityStrategies, SecurityStrategyType.OUTGOING);
    if (!requestProps.isEmpty()) {
      client.getOutInterceptors().add(new WSS4JOutInterceptor(requestProps));
    }

    Map<String, Object> responseProps = buildSecurityProperties(securityStrategies, SecurityStrategyType.INCOMING);
    if (!responseProps.isEmpty()) {
      client.getInInterceptors().add(new WSS4JInInterceptor(responseProps));
    }
  }

  private static Map<String, Object> buildSecurityProperties(List<SecurityStrategyCxfAdapter> strategies,
                                                             SecurityStrategyType type) {
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

  private static void addRequestInterceptors(Client client) {
    List<Interceptor<? extends Message>> outInterceptors = client.getOutInterceptors();
    outInterceptors.add(new SoapActionInterceptor());
  }

  private static void addResponseInterceptors(Client client, boolean mtomEnabled) {
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

  private static void removeUnnecessaryCxfInterceptors(Client client) {
    Binding binding = client.getEndpoint().getBinding();
    removeInterceptor(binding.getOutInterceptors(), WrappedOutInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap11FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), Soap12FaultInInterceptor.class.getName());
    removeInterceptor(binding.getInInterceptors(), CheckFaultInterceptor.class.getName());
  }

  private static void removeInterceptor(List<Interceptor<? extends Message>> inInterceptors, String name) {
    inInterceptors.removeIf(i -> i instanceof PhaseInterceptor && ((PhaseInterceptor) i).getId().equals(name));
  }
}
