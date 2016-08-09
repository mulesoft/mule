/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

import org.mule.compatibility.core.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.security.EndpointSecurityFilter;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class EndpointTestCase extends AbstractMuleTestCase {

  /**
   * Tests that endpoint configuration is done before setting the endpoint in the passed transformers to avoid a race condition
   * when the transformer asks for endpoint's information that has not ben set yet. Related to these issues: EE-1937, MULE-3983
   */
  @Test
  @SuppressWarnings("serial")
  public void testTransformersAreSetupAfterCompleteEndpointConfig() {
    // Defines all the values required in order to create a full configured
    // endpoint
    final Connector mockConnector = mock(Connector.class);
    final EndpointURI uri = mock(EndpointURI.class);
    final List<Transformer> inputTransformers = new ArrayList<>();
    final List<Transformer> outputTransformers = new ArrayList<>();
    final String name = "testEndpoint";

    final Map<String, String> properties = new HashMap<>();
    final String property1 = "property1";
    final String value1 = "value1";
    properties.put(property1, value1);

    final TransactionConfig mockTransactionConfig = mock(TransactionConfig.class);
    final boolean deleteUnacceptedMessages = true;
    final EndpointSecurityFilter mockEndpointSecurityFilter = mock(EndpointSecurityFilter.class);
    final MessageExchangePattern messageExchangePattern = MessageExchangePattern.REQUEST_RESPONSE;
    final int responseTimeout = 5;
    final String initialState = "state1";
    final Charset endpointEncoding = US_ASCII;
    final String endpointBuilderName = "builderName1";
    final MuleContext muleContext = mock(MuleContext.class);
    final RetryPolicyTemplate retryPolicyTemplate = mock(RetryPolicyTemplate.class);
    final AbstractRedeliveryPolicy redeliveryPolicy = mock(IdempotentRedeliveryPolicy.class);
    final EndpointMessageProcessorChainFactory messageProcessorsFactory = mock(EndpointMessageProcessorChainFactory.class);
    final List<MessageProcessor> messageProcessors = new ArrayList<>();
    final List<MessageProcessor> responseMessageProcessors = new ArrayList<>();
    final MediaType mimeType = MediaType.TEXT;
    final boolean disableTransportTransformer = true;

    // Creates a mock Transformer that will check that the endpoint is completely
    // configured when setEndpoint method is called.
    Transformer mockTransformer = mock(Transformer.class, withSettings().extraInterfaces(EndpointAware.class));
    doAnswer(invocation -> {
      AbstractEndpoint endpoint = (AbstractEndpoint) invocation.getArguments()[0];
      assertEquals(mockConnector, endpoint.getConnector());
      assertEquals(uri, endpoint.getEndpointURI());
      assertEquals(name, endpoint.getName());
      assertEquals(value1, endpoint.getProperties().get(property1));
      assertEquals(mockTransactionConfig, endpoint.getTransactionConfig());
      assertEquals(deleteUnacceptedMessages, endpoint.isDeleteUnacceptedMessages());
      assertEquals(mockEndpointSecurityFilter, endpoint.getSecurityFilter());
      assertEquals(messageExchangePattern, endpoint.getExchangePattern());
      assertEquals(responseTimeout, endpoint.getResponseTimeout());
      assertEquals(initialState, endpoint.getInitialState());
      assertEquals(endpointEncoding, endpoint.getEncoding());
      assertEquals(endpointBuilderName, endpoint.getEndpointBuilderName());
      assertEquals(muleContext, endpoint.getMuleContext());
      assertEquals(retryPolicyTemplate, endpoint.getRetryPolicyTemplate());
      assertEquals(redeliveryPolicy, endpoint.getRedeliveryPolicy());
      assertEquals(mimeType, endpoint.getMimeType());
      assertEquals(disableTransportTransformer, endpoint.isDisableTransportTransformer());

      return null;
    }).when((EndpointAware) mockTransformer).setEndpoint(any(ImmutableEndpoint.class));

    inputTransformers.add(mockTransformer);
    outputTransformers.add(mockTransformer);

    // Creates the endpoint using the transformers which will validate the
    // configuration
    new AbstractEndpoint(mockConnector, uri, name, properties, mockTransactionConfig, deleteUnacceptedMessages,
                         messageExchangePattern, responseTimeout, initialState, endpointEncoding, endpointBuilderName,
                         muleContext, retryPolicyTemplate, redeliveryPolicy, messageProcessorsFactory, messageProcessors,
                         responseMessageProcessors, disableTransportTransformer, mimeType) {

      @Override
      protected MessageProcessor createMessageProcessorChain(FlowConstruct flowConstruct) throws MuleException {
        return null;
      }
    };
  }
}
