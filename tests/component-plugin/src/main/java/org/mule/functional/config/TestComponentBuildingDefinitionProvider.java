/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.config;

import static org.mule.functional.config.TestXmlNamespaceInfoProvider.TEST_NAMESPACE;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromTextContent;
import static org.mule.runtime.dsl.api.component.CommonTypeConverters.stringToClassConverter;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.functional.api.component.AssertionMessageProcessor;
import org.mule.functional.api.component.EventCallback;
import org.mule.functional.api.component.FunctionalTestComponent;
import org.mule.functional.api.component.InvocationCountMessageProcessor;
import org.mule.functional.api.component.ResponseAssertionMessageProcessor;
import org.mule.functional.api.component.SharedConfig;
import org.mule.functional.api.component.SharedSource;
import org.mule.functional.api.component.SkeletonSource;
import org.mule.functional.api.component.ThrowProcessor;
import org.mule.functional.client.QueueWriterMessageProcessor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.tck.processor.TestNonBlockingProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider for {@code ComponentBuildingDefinition}s to parse TEST module configuration.
 *
 * @since 4.0
 */
public class TestComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private ComponentBuildingDefinition.Builder baseDefinition;

  @Override
  public void init() {
    baseDefinition = new ComponentBuildingDefinition.Builder().withNamespace(TEST_NAMESPACE);
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> componentBuildingDefinitions = new ArrayList<>();
    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("queue")
        .withTypeDefinition(fromType(QueueWriterMessageProcessor.class))
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("content", fromChildConfiguration(String.class).build())
        .withSetterParameterDefinition("contentJavaType",
                                       fromSimpleParameter("contentJavaType", stringToClassConverter())
                                           .build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("content")
        .withTypeDefinition(fromType(String.class))
        .build());

    ComponentBuildingDefinition.Builder baseComponentDefinition = baseDefinition.copy()
        .withSetterParameterDefinition("eventCallback", fromChildConfiguration(EventCallback.class).build())
        .withSetterParameterDefinition("returnData", fromChildConfiguration(Object.class).build())
        .withSetterParameterDefinition("throwException", fromSimpleParameter("throwException").build())
        .withSetterParameterDefinition("logMessageDetails", fromSimpleParameter("logMessageDetails").build())
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
        .withTypeDefinition(fromType(FunctionalTestComponent.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("throw")
        .withTypeDefinition(fromType(ThrowProcessor.class))
        .withSetterParameterDefinition("exception", fromSimpleParameter("exception").build())
        .withSetterParameterDefinition("error", fromSimpleParameter("error").build())
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
        .withSetterParameterDefinition("responseSameTask", fromSimpleParameter("responseSameTask").build())
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("non-blocking-processor")
        .withTypeDefinition(fromType(TestNonBlockingProcessor.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("skeleton-source")
        .withTypeDefinition(fromType(SkeletonSource.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("shared-source")
        .withSetterParameterDefinition("config", fromSimpleReferenceParameter("config-ref").build())
        .withTypeDefinition(fromType(SharedSource.class))
        .build());

    componentBuildingDefinitions.add(baseDefinition.copy()
        .withIdentifier("shared-config")
        .withTypeDefinition(fromType(SharedConfig.class))
        .build());

    return componentBuildingDefinitions;
  }
}
