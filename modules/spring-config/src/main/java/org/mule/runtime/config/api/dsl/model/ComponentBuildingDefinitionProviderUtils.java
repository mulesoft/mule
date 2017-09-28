/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromMultipleDefinitions;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair.newBuilder;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.dsl.ConfigurableInstanceFactory;
import org.mule.runtime.config.api.dsl.ConfigurableObjectFactory;
import org.mule.runtime.config.api.dsl.processor.TransformerConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.KeyAttributeDefinitionPair;

/**
 * Provides reusable base definition builders for plugin parsers
 * 
 * @since 4.0
 */
public class ComponentBuildingDefinitionProviderUtils {

  public static ComponentBuildingDefinition.Builder getTransformerBaseBuilder(Class<? extends AbstractTransformer> transformerClass,
                                                                              KeyAttributeDefinitionPair... configurationAttributes) {
    return getTransformerBaseBuilder(parameters -> createNewInstance(transformerClass),
                                     transformerClass,
                                     configurationAttributes);
  }

  public static ComponentBuildingDefinition.Builder getTransformerBaseBuilder(ConfigurableInstanceFactory configurableInstanceFactory,
                                                                              Class<? extends Transformer> transformerClass,
                                                                              KeyAttributeDefinitionPair... configurationAttributes) {
    KeyAttributeDefinitionPair[] commonTransformerParameters = {newBuilder()
        .withKey("encoding")
        .withAttributeDefinition(fromSimpleParameter("encoding").build())
        .build(),
        newBuilder()
            .withKey("name")
            .withAttributeDefinition(fromSimpleParameter("name").build())
            .build(),
        newBuilder()
            .withKey("ignoreBadInput")
            .withAttributeDefinition(fromSimpleParameter("ignoreBadInput").build())
            .build(),
        newBuilder()
            .withKey("mimeType")
            .withAttributeDefinition(fromSimpleParameter("mimeType").build())
            .build(),
        newBuilder()
            .withKey("returnClass")
            .withAttributeDefinition(fromSimpleParameter("returnClass").build())
            .build(),
        newBuilder()
            .withKey("muleContext")
            .withAttributeDefinition(fromReferenceObject(MuleContext.class).build())
            .build()};
    return new ComponentBuildingDefinition.Builder()
        .withTypeDefinition(fromType(transformerClass))
        .withObjectFactoryType(new ConfigurableObjectFactory<>().getClass())
        .withSetterParameterDefinition("factory", fromFixedValue(configurableInstanceFactory).build())
        .withSetterParameterDefinition("commonConfiguratorType", fromFixedValue(TransformerConfigurator.class).build())
        .withSetterParameterDefinition("parameters",
                                       fromMultipleDefinitions(addAll(commonTransformerParameters, configurationAttributes))
                                           .build())
        .asPrototype();
  }

  public static ComponentBuildingDefinition.Builder getMuleMessageTransformerBaseBuilder() {
    return new ComponentBuildingDefinition.Builder()
        .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
        .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
        .asPrototype();
  }

  public static Object createNewInstance(Class classType) {
    try {
      return ClassUtils.instantiateClass(classType);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  public static Object createNewInstance(String className) {
    try {
      return ClassUtils.instantiateClass(className, new Object[0]);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

}
