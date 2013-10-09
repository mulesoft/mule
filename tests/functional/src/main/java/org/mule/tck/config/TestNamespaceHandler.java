/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.tck.functional.AssertionMessageProcessor;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.tck.testmodels.services.TestServiceComponent;
import org.mule.tck.transformer.NoActionTransformer;

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
        registerBeanDefinitionParser("no-action-transformer", new MessageProcessorDefinitionParser(NoActionTransformer.class));
        registerMuleBeanDefinitionParser("assert", new MessageProcessorDefinitionParser(AssertionMessageProcessor.class));
    }
}
