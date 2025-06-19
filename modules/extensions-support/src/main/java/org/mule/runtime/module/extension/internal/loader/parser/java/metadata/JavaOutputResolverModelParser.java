/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.metadata;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.getClassName;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isNullResolver;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.loader.parser.metadata.OutputResolverModelParser;
import org.mule.runtime.module.extension.internal.metadata.MuleOutputTypeResolverAdapter;

/**
 * {@link OutputResolverModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaOutputResolverModelParser implements OutputResolverModelParser {

  private final Class<?> outputTypeResolverDeclarationClass;
  private final LazyValue<OutputTypeResolver<?>> instance;

  public JavaOutputResolverModelParser(Class<?> outputTypeResolverDeclarationClass) {
    this.outputTypeResolverDeclarationClass = outputTypeResolverDeclarationClass;
    this.instance = new LazyValue<>(() -> instantiateResolver(outputTypeResolverDeclarationClass));
  }

  public JavaOutputResolverModelParser(OutputTypeResolver<?> outputTypeResolver) {
    this.outputTypeResolverDeclarationClass = outputTypeResolver.getClass();
    this.instance = new LazyValue<>(outputTypeResolver);
  }

  public boolean hasOutputResolver() {
    return !isNullResolver(outputTypeResolverDeclarationClass);
  }

  public OutputTypeResolver getOutputResolver() {
    return instance.get();
  }

  private OutputTypeResolver instantiateResolver(Class<?> factoryType) {
    try {
      Object resolver = ClassUtils.instantiateClass(factoryType);
      if (resolver instanceof org.mule.sdk.api.metadata.resolving.OutputTypeResolver) {
        return MuleOutputTypeResolverAdapter.from(resolver);
      } else {
        return (OutputTypeResolver) resolver;
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of type " + getClassName(factoryType)), e);
    }
  }

}
