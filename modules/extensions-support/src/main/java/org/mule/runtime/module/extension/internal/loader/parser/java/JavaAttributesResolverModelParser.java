/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.getClassName;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaMetadataTypeResolverUtils.isNullResolver;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.metadata.MuleAttributesTypeResolverAdapter;

/**
 * {@link AttributesResolverModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaAttributesResolverModelParser implements AttributesResolverModelParser {

  private final Class<?> attributesTypeResolverDeclarationClass;

  public JavaAttributesResolverModelParser(Class<?> attributesTypeResolverDeclarationClass) {
    this.attributesTypeResolverDeclarationClass = attributesTypeResolverDeclarationClass;
  }

  public boolean hasAttributesResolver() {
    return !isNullResolver(attributesTypeResolverDeclarationClass);
  }

  public AttributesTypeResolver getAttributesResolver() {
    return instantiateResolver(attributesTypeResolverDeclarationClass);
  }

  private AttributesTypeResolver instantiateResolver(Class<?> factoryType) {
    try {
      Object resolver = ClassUtils.instantiateClass(factoryType);
      if (resolver instanceof org.mule.sdk.api.metadata.resolving.AttributesTypeResolver) {
        return MuleAttributesTypeResolverAdapter.from(resolver);
      } else {
        return (AttributesTypeResolver) resolver;
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of type " + getClassName(factoryType)), e);
    }
  }
}
