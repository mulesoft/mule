/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.processor;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.config.api.dsl.ObjectFactoryCommonConfigurator;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.util.Map;

/**
 * {@link ObjectFactoryCommonConfigurator} for transformers in Mule.
 *
 * The transformer class that will be configured setting the returnType, mimeType, name, encoding and ignoreBadInput
 * configuration.
 *
 * @since 4.0
 */
public class TransformerConfigurator implements ObjectFactoryCommonConfigurator<AbstractTransformer> {

  private Class<?> getReturnType(String returnClass) {
    Class<?> returnType = Object.class;
    if (returnClass != null) {
      try {
        returnType = org.apache.commons.lang3.ClassUtils.getClass(returnClass);
      } catch (ClassNotFoundException e) {
        throw new MuleRuntimeException(e);
      }
    }
    return returnType;
  }

  /**
   * Configures the common parameters of every transformer.
   *
   * @param transformerInstance the transformar instance
   * @param parameters the set of parameters configured in the component model according to the
   *        {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}
   */
  @Override
  public void configure(AbstractTransformer transformerInstance, Map<String, Object> parameters) {
    String returnClass = (String) parameters.get("returnClass");
    String mimeType = (String) parameters.get("mimeType");
    String name = (String) parameters.get("name");
    String encoding = (String) parameters.get("encoding");
    Boolean ignoreBadInput =
        parameters.get("ignoreBadInput") == null ? null : Boolean.valueOf((String) parameters.get("ignoreBadInput"));
    if (returnClass != null || mimeType != null) {
      DataTypeParamsBuilder builder = DataType.builder().type(getReturnType(returnClass));
      if (isNotEmpty(mimeType)) {
        builder.mediaType(mimeType);
      }
      transformerInstance.setReturnDataType(builder.charset(encoding).build());
    }
    if (ignoreBadInput != null) {
      transformerInstance.setIgnoreBadInput(ignoreBadInput);
    }
    transformerInstance.setName(name);
  }

}
