/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransformerMessageProcessorDefinitionParser;
import org.mule.runtime.core.endpoint.URIBuilder;
import org.mule.functional.functional.AssertionMessageProcessor;
import org.mule.functional.functional.InvocationCountMessageProcessor;
import org.mule.functional.functional.ResponseAssertionMessageProcessor;
import org.mule.functional.functional.TestNonBlockingProcessor;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.functional.testmodels.services.TestServiceComponent;
import org.mule.functional.transformer.NoActionTransformer;

public class TestNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        registerStandardTransportEndpoints(TestConnector.TEST, URIBuilder.PATH_ATTRIBUTES);
        registerConnectorDefinitionParser(TestConnector.class);

        registerBeanDefinitionParser("component", new TestComponentDefinitionParser());
        registerBeanDefinitionParser("web-service-component", new TestComponentDefinitionParser(TestServiceComponent.class));
        //This is handled by the TestComponentDefinitionParser
        registerIgnoredElement("return-data");
        registerIgnoredElement("callback");
        registerBeanDefinitionParser("no-action-transformer", new TransformerMessageProcessorDefinitionParser(NoActionTransformer.class));
        registerMuleBeanDefinitionParser("assert", new MessageProcessorDefinitionParser(AssertionMessageProcessor.class));
        registerMuleBeanDefinitionParser("invocation-counter", new MessageProcessorDefinitionParser(InvocationCountMessageProcessor.class));
        registerMuleBeanDefinitionParser("non-blocking-processor", new MessageProcessorDefinitionParser(TestNonBlockingProcessor.class));
        registerMuleBeanDefinitionParser("assert-intercepting", new MessageProcessorDefinitionParser(ResponseAssertionMessageProcessor.class));
        registerBeanDefinitionParser("queue",  new QueueWriterMessageProcessorBeanDefinitionParser());
    }
}
