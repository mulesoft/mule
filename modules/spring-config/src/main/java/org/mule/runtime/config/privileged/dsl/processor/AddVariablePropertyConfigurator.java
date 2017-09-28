/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.privileged.dsl.processor;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.config.api.dsl.ObjectFactoryCommonConfigurator;
import org.mule.runtime.core.privileged.processor.simple.AbstractAddVariablePropertyProcessor;

import java.util.Map;

/**
 * {@link ObjectFactoryCommonConfigurator} for transformers in Mule.
 *
 * The transformer class that will be configured setting the returnType, mimeType, name, encoding and ignoreBadInput
 * configuration.
 *
 * @since 4.0
 */
public class AddVariablePropertyConfigurator implements ObjectFactoryCommonConfigurator<AbstractAddVariablePropertyProcessor> {

  /**
   * Configures the common parameters of every transformer.
   *
   * @param propVarSetterInstance the transformar instance
   * @param parameters the set of parameters configured in the component model according to the
   *        {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}
   */
  @Override
  public void configure(AbstractAddVariablePropertyProcessor propVarSetterInstance, Map<String, Object> parameters) {
    String mimeType = (String) parameters.get("mimeType");
    String encoding = (String) parameters.get("encoding");
    if (mimeType != null) {
      DataTypeParamsBuilder builder = DataType.builder();
      if (isNotEmpty(mimeType)) {
        builder.mediaType(mimeType);
      }
      propVarSetterInstance.setReturnDataType(builder.charset(encoding).build());
    }
  }

}
