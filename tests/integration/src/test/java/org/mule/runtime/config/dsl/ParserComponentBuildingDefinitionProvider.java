/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl;

import static org.mule.runtime.config.dsl.ParserXmlNamespaceInfoProvider.PARSERS_TEST_NAMESACE;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.parsers.beans.SimpleCollectionObject;
import org.mule.runtime.core.api.MuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class ParserComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider
{

    @Override
    public void init(MuleContext muleContext)
    {
    }

    @Override
    public List<ComponentBuildingDefinition> getComponentBuildingDefinitions()
    {
        List<ComponentBuildingDefinition> definitions = new ArrayList<>();
        ComponentBuildingDefinition.Builder baseBuilder = new ComponentBuildingDefinition.Builder()
                .withNamespace(PARSERS_TEST_NAMESACE);
        definitions.add(baseBuilder
                                .copy()
                                .withIdentifier("parameter-collection-parser")
                                .withTypeDefinition(fromType(SimpleCollectionObject.class))
                                .withSetterParameterDefinition("simpleTypeList", fromChildCollectionConfiguration(String.class).withWrapperIdentifier("simple-type-child-list").build())
                                .withSetterParameterDefinition("simpleTypeSet", fromChildCollectionConfiguration(String.class).withWrapperIdentifier("simple-type-child-set").withCollectionType(HashSet.class).build())
                                .withSetterParameterDefinition("simpleParameters", fromMultipleDefinitions(
                                        newBuilder()
                                                .withAttributeDefinition(fromSimpleParameter("firstname").build())
                                                .withKey("firstname")
                                                .build(),
                                        newBuilder()
                                                .withAttributeDefinition(fromSimpleParameter("lastname").build())
                                                .withKey("lastname")
                                                .build(),
                                        newBuilder()
                                                .withAttributeDefinition(fromSimpleParameter("age").build())
                                                .withKey("age")
                                                .build(),
                                        newBuilder()
                                                .withAttributeDefinition(fromChildConfiguration(SimpleCollectionObject.class).withWrapperIdentifier("first-child").build())
                                                .withKey("first-child")
                                                .build(),
                                        newBuilder()
                                                .withAttributeDefinition(fromChildConfiguration(SimpleCollectionObject.class).withWrapperIdentifier("second-child").build())
                                                .withKey("second-child")
                                                .build(),
                                        newBuilder()
                                                .withAttributeDefinition(fromChildCollectionConfiguration(SimpleCollectionObject.class).withWrapperIdentifier("other-children").build())
                                                .withKey("other-children")
                                                .build(),
                                        newBuilder()
                                                .withAttributeDefinition(fromChildCollectionConfiguration(SimpleCollectionObject.class).withWrapperIdentifier("other-children-custom-collection-type").withCollectionType(LinkedList.class).build())
                                                .withKey("other-children-custom-collection-type")
                                                .build(),
                                        newBuilder()
                                                .withAttributeDefinition(fromChildCollectionConfiguration(String.class).withWrapperIdentifier("other-simple-type-child-list").build())
                                                .withKey("other-simple-type-child-list-custom-key")
                                                .build())
                                        .build())
                                .build());

        definitions.add(baseBuilder
                                .copy()
                                .withIdentifier("simple-type-child")
                                .withTypeDefinition(fromType(String.class))
                                .build());

        return definitions;
    }
}
