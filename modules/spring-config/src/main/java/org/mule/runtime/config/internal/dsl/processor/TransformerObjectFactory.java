/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.processor;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;
import org.mule.runtime.dsl.api.component.ObjectFactory;

/**
 * {@link ObjectFactory} for transformer in Mules.
 *
 * The transformer class that will be created need to be provided by setting the {@code transformerClass} attribute. The
 * recommended approach is to make use of
 * {@link org.mule.runtime.config.internal.dsl.model.CoreComponentBuildingDefinitionProvider#getTransformerBaseBuilderForClass(Class)}
 * or {@code org.mule.runtime.config.dsl.model.CoreComponentBuildingDefinitionProvider#getTransformerBaseBuilder()}.
 *
 * This class can also be used as the base class for other {@code ObjectFactory}s that need to inject additional field to the
 * created transformer. The {@code createInstance} method can be override to create the transformer instance and the
 * {@code postProcessInstance} method can be used to do additional stuff over the transformer instance like doing additional
 * parameter configuration.
 */
public class TransformerObjectFactory extends AbstractComponentFactory<Transformer> {

  private Class<? extends AbstractTransformer> transformerClass;
  private String name;
  private boolean ignoreBadInput;
  private String returnClass;
  private String encoding;
  private String mimeType;

  @Override
  public final Transformer doGetObject() throws Exception {
    AbstractTransformer transformerInstance = createInstance();
    if (returnClass != null || mimeType != null) {
      DataTypeParamsBuilder builder = DataType.builder().type(getReturnType());
      if (isNotEmpty(mimeType)) {
        builder.mediaType(mimeType);
      }
      transformerInstance.setReturnDataType(builder.charset(encoding).build());
    }
    transformerInstance.setIgnoreBadInput(ignoreBadInput);
    transformerInstance.setName(name);
    postProcessInstance(transformerInstance);
    return transformerInstance;
  }

  /**
   * Template method for subclasses to customize the transformer instance.
   *
   * @param transformerInstance the instantiated transformer instance with the basic configuration.
   */
  protected void postProcessInstance(AbstractTransformer transformerInstance) {}

  /**
   * Template method for creating the transformer instance.
   *
   * @return a transformer instance
   * @throws NoSuchMethodException
   * @throws InstantiationException
   * @throws IllegalAccessException
   * @throws java.lang.reflect.InvocationTargetException
   */
  protected AbstractTransformer createInstance() {
    checkArgument(transformerClass != null, "Default createInstance method is used but no transformerClass was provided");
    try {
      return ClassUtils.instantiateClass(transformerClass, new Object[0]);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Class<?> getReturnType() {
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

  public void setTransformerClass(Class<? extends AbstractTransformer> transformerClass) {
    this.transformerClass = transformerClass;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setReturnClass(String returnClass) {
    this.returnClass = returnClass;
  }

  public void setIgnoreBadInput(boolean ignoreBadInput) {
    this.ignoreBadInput = ignoreBadInput;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }
}
