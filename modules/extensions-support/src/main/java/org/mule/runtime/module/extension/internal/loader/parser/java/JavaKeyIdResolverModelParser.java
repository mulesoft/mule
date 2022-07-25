/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.metadata.api.utils.MetadataTypeUtils.isEnum;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.getClassName;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.module.extension.internal.loader.parser.KeyIdResolverModelParser;
import org.mule.runtime.module.extension.internal.metadata.BooleanKeyResolver;
import org.mule.runtime.module.extension.internal.metadata.EnumKeyResolver;
import org.mule.runtime.module.extension.internal.metadata.SdkTypeKeysResolverAdapter;
import org.mule.sdk.api.metadata.resolving.PartialTypeKeysResolver;

/**
 * {@link KeyIdResolverModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaKeyIdResolverModelParser implements KeyIdResolverModelParser {

  private final MetadataType metadataType;
  private final String categoryName;
  private final Class<?> keyIdResolverDeclarationClass;
  private final boolean muleResolver;

  public JavaKeyIdResolverModelParser(String categoryName, MetadataType metadataType, Class<?> keyIdResolverDeclarationClass,
                                      boolean muleResolver) {
    this.metadataType = metadataType;
    this.categoryName = categoryName;
    this.keyIdResolverDeclarationClass = keyIdResolverDeclarationClass;
    this.muleResolver = muleResolver;
  }

  @Override
  public boolean hasKeyIdResolver() {
    return (!org.mule.sdk.api.metadata.NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass) &&
        !org.mule.runtime.extension.api.metadata.NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass)) ||
        ((metadataType != null) && ((metadataType instanceof BooleanType) || isEnum(metadataType)));
  }

  @Override
  public boolean isPartialKeyResolver() {
    return PartialTypeKeysResolver.class.isAssignableFrom(keyIdResolverDeclarationClass) ||
        org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver.class.isAssignableFrom(keyIdResolverDeclarationClass);
  }

  @Override
  public org.mule.sdk.api.metadata.resolving.TypeKeysResolver getKeyResolver() {
    org.mule.sdk.api.metadata.resolving.TypeKeysResolver typeKeysResolver;
    if (metadataType == null || categoryName == null) {
      typeKeysResolver = instantiateResolver();
    } else if (NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass) ||
        org.mule.sdk.api.metadata.NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass)) {
      if (metadataType instanceof BooleanType) {
        typeKeysResolver = new BooleanKeyResolver(categoryName);
      } else if (isEnum(metadataType)) {
        typeKeysResolver = new EnumKeyResolver(metadataType.getAnnotation(EnumAnnotation.class).get(), categoryName);
      } else {
        typeKeysResolver = instantiateResolver();
      }
    } else {
      typeKeysResolver = instantiateResolver();
    }

    return typeKeysResolver;
  }

  private org.mule.sdk.api.metadata.resolving.TypeKeysResolver instantiateResolver() {
    try {
      Object resolver = ClassUtils.instantiateClass(keyIdResolverDeclarationClass);
      if (muleResolver) {
        return new SdkTypeKeysResolverAdapter((org.mule.runtime.api.metadata.resolving.TypeKeysResolver) resolver);
      } else {
        return (org.mule.sdk.api.metadata.resolving.TypeKeysResolver) resolver;
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of type "
          + getClassName(keyIdResolverDeclarationClass)), e);
    }
  }

}
