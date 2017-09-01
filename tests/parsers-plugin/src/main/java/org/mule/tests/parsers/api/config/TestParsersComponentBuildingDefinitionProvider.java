/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api.config;

import static java.lang.Integer.valueOf;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromChildMapConfiguration;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromMapEntryType;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.privileged.processor.CompositeProcessorChainRouter;
import org.mule.runtime.core.privileged.processor.ProcessorChainRouter;
import org.mule.runtime.core.privileged.processor.objectfactory.MessageProcessorChainObjectFactory;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.tests.parsers.api.LifecycleSensingMessageProcessor;
import org.mule.tests.parsers.api.LifecycleSensingObjectFactory;
import org.mule.tests.parsers.api.ParameterAndChildElement;
import org.mule.tests.parsers.api.ParsersTestObject;
import org.mule.tests.parsers.api.PojoWithSameTypeChildren;
import org.mule.tests.parsers.api.SimplePojo;
import org.mule.tests.parsers.api.TestObject;
import org.mule.tests.parsers.api.TestObjectFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class TestParsersComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  @Override
  public void init() {}

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {
    List<ComponentBuildingDefinition> definitions = new ArrayList<>();
    ComponentBuildingDefinition.Builder baseBuilder =
        new ComponentBuildingDefinition.Builder().withNamespace(TestParsersNamespaceInfoProvider.PARSERS_TEST_NAMESPACE);

    ComponentBuildingDefinition.Builder baseParameterCollectionParserBuilder = baseBuilder
        .withTypeDefinition(fromType(ParsersTestObject.class)).asNamed()
        .withSetterParameterDefinition("simpleTypeWithConverter", fromChildConfiguration(String.class).build())
        .withSetterParameterDefinition("simpleTypeList",
                                       fromChildConfiguration(List.class).withWrapperIdentifier("simple-type-child-list").build())
        .withSetterParameterDefinition("simpleTypeListWithConverter",
                                       fromChildConfiguration(List.class)
                                           .withWrapperIdentifier("simple-type-child-list-with-converter").build())
        .withSetterParameterDefinition("simpleTypeSet",
                                       fromChildConfiguration(Set.class).withWrapperIdentifier("simple-type-child-set").build())
        .withSetterParameterDefinition("simpleTypeMap",
                                       fromChildMapConfiguration(String.class, Integer.class)
                                           .withWrapperIdentifier("simple-type-map").build())
        .withSetterParameterDefinition("simpleListTypeMap",
                                       fromChildMapConfiguration(String.class, String.class)
                                           .withWrapperIdentifier("simple-list-type-map").build())
        .withSetterParameterDefinition("complexTypeMap",
                                       fromChildMapConfiguration(Long.class, ParsersTestObject.class)
                                           .withWrapperIdentifier("complex-type-map").build())
        .withSetterParameterDefinition("simpleParameters", fromMultipleDefinitions(newBuilder()
            .withAttributeDefinition(fromSimpleParameter("firstname").build()).withKey("firstname").build(),
                                                                                   newBuilder()
                                                                                       .withAttributeDefinition(fromSimpleParameter("lastname")
                                                                                           .build())
                                                                                       .withKey("lastname").build(),
                                                                                   newBuilder()
                                                                                       .withAttributeDefinition(fromSimpleParameter("age")
                                                                                           .build())
                                                                                       .withKey("age").build(),
                                                                                   newBuilder()
                                                                                       .withAttributeDefinition(fromChildConfiguration(ParsersTestObject.class)
                                                                                           .withWrapperIdentifier("first-child")
                                                                                           .build())
                                                                                       .withKey("first-child").build(),
                                                                                   newBuilder()
                                                                                       .withAttributeDefinition(fromChildConfiguration(ParsersTestObject.class)
                                                                                           .withWrapperIdentifier("second-child")
                                                                                           .build())
                                                                                       .withKey("second-child").build(),
                                                                                   newBuilder()
                                                                                       .withAttributeDefinition(fromChildConfiguration(List.class)
                                                                                           .withWrapperIdentifier("other-children")
                                                                                           .build())
                                                                                       .withKey("other-children").build(),
                                                                                   newBuilder()
                                                                                       .withAttributeDefinition(fromChildConfiguration(List.class)
                                                                                           .withWrapperIdentifier("other-children-custom-collection-type")
                                                                                           .build())
                                                                                       .withKey("other-children-custom-collection-type")
                                                                                       .build(),
                                                                                   newBuilder()
                                                                                       .withAttributeDefinition(fromChildConfiguration(List.class)
                                                                                           .withWrapperIdentifier("other-simple-type-child-list")
                                                                                           .build())
                                                                                       .withKey("other-simple-type-child-list-custom-key")
                                                                                       .build()).build());

    definitions.add(baseParameterCollectionParserBuilder.withIdentifier("parameter-collection-parser").build());

    definitions.add(baseParameterCollectionParserBuilder.withIdentifier("elementTypeA").build());

    definitions.add(baseParameterCollectionParserBuilder.withIdentifier("anotherElementTypeA").build());

    definitions.add(baseBuilder.withIdentifier("simple-type-child-list").withTypeDefinition(fromType(List.class)).build());

    definitions.add(baseBuilder.withIdentifier("simple-type-child-list-with-converter")
        .withTypeDefinition(fromType(List.class)).build());

    definitions.add(baseBuilder.withIdentifier("other-children-custom-collection-type")
        .withTypeDefinition(fromType(LinkedList.class)).build());

    definitions.add(baseBuilder.withIdentifier("other-children").withTypeDefinition(fromType(List.class)).build());

    definitions
        .add(baseBuilder.withIdentifier("simple-type-child-set").withTypeDefinition(fromType(TreeSet.class)).build());

    definitions
        .add(baseBuilder.withIdentifier("other-simple-type-child-list").withTypeDefinition(fromType(List.class)).build());

    definitions.add(baseBuilder.withIdentifier("simple-type-child").withTypeDefinition(fromType(String.class)).build());

    definitions.add(baseBuilder.withIdentifier("simple-type-child-with-converter")
        .withTypeDefinition(fromType(String.class)).withTypeConverter(input -> input + "-with-converter").build());

    definitions.add(baseBuilder.withIdentifier("simple-type-map").withTypeDefinition(fromType(TreeMap.class)).build());

    definitions.add(baseBuilder.withIdentifier("simple-type-entry")
        .withTypeDefinition(fromMapEntryType(String.class, Integer.class))
        .withKeyTypeConverter(input -> input + "-with-converter").withTypeConverter(input -> valueOf((String) input) + 1)
        .build());

    definitions.add(baseBuilder.withIdentifier("simple-list-type-map").withTypeDefinition(fromType(Map.class)).build());

    definitions.add(baseBuilder.withIdentifier("simple-list-entry")
        .withTypeDefinition(fromMapEntryType(String.class, List.class)).build());

    definitions.add(baseBuilder.withIdentifier("complex-type-map").withTypeDefinition(fromType(Map.class)).build());

    definitions.add(baseBuilder.withIdentifier("complex-type-entry")
        .withTypeDefinition(fromMapEntryType(Long.class, ParsersTestObject.class)).build());

    definitions.add(baseBuilder.withIdentifier("global-element-with-object-factory")
        .withTypeDefinition(fromType(LifecycleSensingMessageProcessor.class))
        .withObjectFactoryType(LifecycleSensingObjectFactory.class).build());

    definitions.add(baseBuilder.withIdentifier("inner-element-with-object-factory")
        .withTypeDefinition(fromType(LifecycleSensingMessageProcessor.class))
        .withObjectFactoryType(LifecycleSensingObjectFactory.class).build());

    definitions.add(baseBuilder.withIdentifier("element-with-attribute-and-child")
        .withTypeDefinition(fromType(ParameterAndChildElement.class))
        .withSetterParameterDefinition("simplePojo",
                                       fromSimpleParameter("myPojo", input -> new SimplePojo((String) input))
                                           .withDefaultValue("jose").build())
        .withSetterParameterDefinition("simplePojo", fromChildConfiguration(SimplePojo.class).build()).build());

    definitions.add(baseBuilder.withIdentifier("my-pojo").withTypeDefinition(fromType(SimplePojo.class))
        .withSetterParameterDefinition("someParameter", fromSimpleParameter("someParameter").build()).build());

    definitions.add(baseBuilder.withIdentifier("text-pojo").withTypeDefinition(fromType(SimplePojo.class))
        .withSetterParameterDefinition("someParameter", fromChildConfiguration(String.class).withIdentifier("text").build())
        .build());

    definitions.add(baseBuilder.withIdentifier("text").withTypeDefinition(fromType(String.class)).build());

    definitions.add(baseBuilder.withIdentifier("same-child-type-container")
        .withTypeDefinition(fromType(PojoWithSameTypeChildren.class))
        .withSetterParameterDefinition("elementTypeA",
                                       fromChildConfiguration(ParsersTestObject.class).withIdentifier("elementTypeA")
                                           .build())
        .withSetterParameterDefinition("anotherElementTypeA", fromChildConfiguration(ParsersTestObject.class)
            .withIdentifier("anotherElementTypeA").build())
        .build());

    definitions.add(baseBuilder.withIdentifier("simple-type").withTypeConverter(o -> new SimplePojo((String) o))
        .withTypeDefinition(fromType(String.class)).build());

    definitions.add(baseBuilder.withIdentifier("component-created-with-object-factory")
        .withObjectFactoryType(TestObjectFactory.class)
        .withTypeDefinition(fromType(TestObject.class)).build());

    definitions.add(baseBuilder.withIdentifier("composite-processor-chain-router")
        .withTypeDefinition(fromType(CompositeProcessorChainRouter.class))
        .withSetterParameterDefinition("processorChains", fromChildCollectionConfiguration(Object.class).build())
        .build());

    definitions.add(baseBuilder.withIdentifier("chain")
        .withTypeDefinition(fromType(Component.class))
        .withObjectFactoryType(MessageProcessorChainObjectFactory.class)
        .withSetterParameterDefinition("messageProcessors", fromChildCollectionConfiguration(Object.class).build())
        .build());

    definitions.add(baseBuilder.withIdentifier("processor-chain-router")
        .withTypeDefinition(fromType(ProcessorChainRouter.class))
        .withSetterParameterDefinition("processors", fromChildCollectionConfiguration(Object.class).build())
        .build());

    return definitions;
  }

}

