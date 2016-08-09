/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.config;

import static org.mule.functional.config.TestXmlNamespaceInfoProvider.TEST_NAMESPACE;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromTextContent;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.config.spring.dsl.model.CoreComponentBuildingDefinitionProvider.getTransformerBaseBuilder;

import org.mule.functional.client.QueueWriterMessageProcessor;
import org.mule.functional.functional.AssertionMessageProcessor;
import org.mule.functional.functional.EventCallback;
import org.mule.functional.functional.InvocationCountMessageProcessor;
import org.mule.functional.functional.ResponseAssertionMessageProcessor;
import org.mule.functional.transformer.NoActionTransformer;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.tck.processor.TestNonBlockingProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider for {@code ComponentBuildingDefinition}s to parse TEST module configuration.
 *
 * @since 4.0
 */
public class TestComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider
{

    private ComponentBuildingDefinition.Builder baseDefinition;

    @Override
    public void init(MuleContext muleContext)
    {
        baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace(TEST_NAMESPACE);
    }

    @Override
    public List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    {
        List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("queue")
                                                 .withTypeDefinition(fromType(QueueWriterMessageProcessor.class))
                                                 .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                                                 .build());

        ComponentBuildingDefinition.Builder baseComponentDefinition = baseDefinition.copy()
                .withSetterParameterDefinition("eventCallback", fromChildConfiguration(EventCallback.class).build())
                .withSetterParameterDefinition("returnData", fromChildConfiguration(Object.class).build())
                .withSetterParameterDefinition("throwException", fromSimpleParameter("throwException").build())
                .withSetterParameterDefinition("logMessageDetails", fromSimpleParameter("logMessageDetails").build())
                .withSetterParameterDefinition("doInboundTransform", fromSimpleParameter("doInboundTransform").build())
                .withSetterParameterDefinition("exceptionToThrow", fromSimpleParameter("exceptionToThrow").build())
                .withSetterParameterDefinition("exceptionText", fromSimpleParameter("exceptionText").build())
                .withSetterParameterDefinition("enableMessageHistory", fromSimpleParameter("enableMessageHistory").build())
                .withSetterParameterDefinition("enableNotifications", fromSimpleParameter("enableNotifications").build())
                .withSetterParameterDefinition("appendString", fromSimpleParameter("appendString").build())
                .withSetterParameterDefinition("waitTime", fromSimpleParameter("waitTime").build())
                .withSetterParameterDefinition("id", fromSimpleParameter("id").build())
                .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build());

        componentBuildingDefinitions.add(baseComponentDefinition.copy()
                                                 .withIdentifier("component")
                                                 .withTypeDefinition(fromType(MessageProcessor.class))
                                                 .withObjectFactoryType(FunctionalComponentObjectFactory.class)
                                                 .build());
        
        componentBuildingDefinitions.add(baseComponentDefinition.copy()
                                                 .withIdentifier("web-service-component")
                                                 .withObjectFactoryType(TestFunctionalComponentObjectFactory.class)
                                                 .withTypeDefinition(fromType(MessageProcessor.class))
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("return-data")
                                                 .withTypeDefinition(fromType(Object.class))
                                                 .withObjectFactoryType(ReturnDataObjectFactory.class)
                                                 .withSetterParameterDefinition("file", fromSimpleParameter("file").build())
                                                 .withSetterParameterDefinition("content", fromTextContent().build())
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("callback")
                                                 .withTypeDefinition(fromConfigurationAttribute("class")).build());

        componentBuildingDefinitions.add(getTransformerBaseBuilder(NoActionTransformer.class)
                                                 .withNamespace(TEST_NAMESPACE)
                                                 .withIdentifier("no-action-transformer")
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("assert")
                                                 .withTypeDefinition(fromType(AssertionMessageProcessor.class))
                                                 .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
                                                 .withSetterParameterDefinition("message", fromSimpleParameter("message").build())
                                                 .withSetterParameterDefinition("count", fromSimpleParameter("count").build())
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("invocation-counter")
                                                 .withTypeDefinition(fromType(InvocationCountMessageProcessor.class))
                                                 .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("assert-intercepting")
                                                 .withTypeDefinition(fromType(ResponseAssertionMessageProcessor.class))
                                                 .withSetterParameterDefinition("responseExpression", fromSimpleParameter("responseExpression").build())
                                                 .withSetterParameterDefinition("responseCount", fromSimpleParameter("responseCount").build())
                                                 .withSetterParameterDefinition("responseSameThread", fromSimpleParameter("responseSameThread").build())
                                                 .build());

        componentBuildingDefinitions.add(baseDefinition.copy()
                                                 .withIdentifier("non-blocking-processor")
                                                 .withTypeDefinition(fromType(TestNonBlockingProcessor.class))
                                                 .build());

        return componentBuildingDefinitions;
    }
}
