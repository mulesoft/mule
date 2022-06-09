/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.getClassName;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.module.extension.internal.loader.parser.AttributesResolverModelParser;
import org.mule.runtime.module.extension.internal.metadata.SdkAttributesTypeResolverAdapter;
import org.mule.sdk.api.metadata.NullMetadataResolver;
import org.mule.sdk.api.metadata.resolving.AttributesTypeResolver;

public class JavaAttributesResolverModelParser implements AttributesResolverModelParser {

  private final Class<?> attributesTypeResolverDeclarationClass;
  private final boolean muleResolver;

  public JavaAttributesResolverModelParser(Class<?> attributesTypeResolverDeclarationClass, boolean muleResolver) {
    this.attributesTypeResolverDeclarationClass = attributesTypeResolverDeclarationClass;
    this.muleResolver = muleResolver;
  }

  public boolean isMuleResolver() {
    return muleResolver;
  }

  public boolean hasAttributesResolver() {
    return !NullMetadataResolver.class.isAssignableFrom(attributesTypeResolverDeclarationClass) &&
        !org.mule.runtime.extension.api.metadata.NullMetadataResolver.class
            .isAssignableFrom(attributesTypeResolverDeclarationClass);
  }

  public AttributesTypeResolver getAttributesResolver() {
    return instantiateResolver(attributesTypeResolverDeclarationClass);
  }

  private AttributesTypeResolver instantiateResolver(Class<?> factoryType) {
    try {
      Object resolver = ClassUtils.instantiateClass(factoryType);
      if (muleResolver) {
        return SdkAttributesTypeResolverAdapter.from((org.mule.runtime.api.metadata.resolving.AttributesTypeResolver) resolver);
      } else {
        return (AttributesTypeResolver) resolver;
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of type " + getClassName(factoryType)), e);
    }
  }
}
