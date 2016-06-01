/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl;

import static org.mule.runtime.config.dsl.ParserXmlNamespaceInfoProvider.PARSERS_TEST_NAMESACE;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildListConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.processor.TypeDefinition.fromType;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.parsers.beans.SimpleCollectionObject;
import org.mule.runtime.core.api.MuleContext;

import java.util.ArrayList;
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
        definitions.add(new ComponentBuildingDefinition.Builder()
                                .withNamespace(PARSERS_TEST_NAMESACE)
                                .withIdentifier("parameter-collection-parser")
                                .withTypeDefinition(fromType(SimpleCollectionObject.class))
                                .withSetterParameterDefinition("simpleParameters", fromMultipleDefinitions(fromSimpleParameter("firstname").build(),
                                                                                                           fromSimpleParameter("lastname").build(),
                                                                                                           fromSimpleParameter("age").build(),
                                                                                                           fromChildConfiguration(SimpleCollectionObject.class).withWrapperIdentifier("first-child").build(),
                                                                                                           fromChildConfiguration(SimpleCollectionObject.class).withWrapperIdentifier("second-child").build(),
                                                                                                           fromChildListConfiguration(SimpleCollectionObject.class).withWrapperIdentifier("other-children").build())
                                        .build())
                                .build());
        return definitions;
    }
}
