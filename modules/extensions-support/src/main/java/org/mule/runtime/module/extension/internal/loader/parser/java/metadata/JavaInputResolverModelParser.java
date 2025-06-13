/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.metadata;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.getClassName;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.loader.parser.metadata.InputResolverModelParser;
import org.mule.runtime.module.extension.internal.metadata.MuleInputTypeResolverAdapter;


public class JavaInputResolverModelParser implements InputResolverModelParser {

  private final String parameterName;
  private final Class<?> inputTypeResolverDeclarationClass;

  public JavaInputResolverModelParser(String parameterName, Class<?> inputTypeResolverDeclarationClass) {
    this.parameterName = parameterName;
    this.inputTypeResolverDeclarationClass = inputTypeResolverDeclarationClass;
  }

  public String getParameterName() {
    return parameterName;
  }

  public InputTypeResolver getInputResolver() {
    return instantiateResolver(inputTypeResolverDeclarationClass);
  }

  private InputTypeResolver instantiateResolver(Class<?> factoryType) {
    try {
      Object resolver = ClassUtils.instantiateClass(factoryType);
      if (resolver instanceof org.mule.sdk.api.metadata.resolving.InputTypeResolver) {
        return MuleInputTypeResolverAdapter.from(resolver);
      } else {
        return (InputTypeResolver) resolver;
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of type " + getClassName(factoryType)), e);
    }
  }

}
