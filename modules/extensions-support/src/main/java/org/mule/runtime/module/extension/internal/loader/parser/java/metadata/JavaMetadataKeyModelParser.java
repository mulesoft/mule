/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.metadata;

import static org.mule.metadata.api.utils.MetadataTypeUtils.isEnum;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.getClassName;

import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.loader.parser.metadata.MetadataKeyModelParser;
import org.mule.runtime.module.extension.internal.metadata.BooleanKeyResolver;
import org.mule.runtime.module.extension.internal.metadata.EnumKeyResolver;
import org.mule.runtime.module.extension.internal.metadata.MuleTypeKeysResolverAdapter;
import org.mule.sdk.api.metadata.NullMetadataResolver;
import org.mule.sdk.api.metadata.resolving.PartialTypeKeysResolver;


/**
 * {@link MetadataKeyModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaMetadataKeyModelParser implements MetadataKeyModelParser {

  private final String parameterName;
  private final MetadataType metadataType;
  private final String categoryName;
  private final Class<?> keyIdResolverDeclarationClass;

  public JavaMetadataKeyModelParser(String parameterName, String categoryName, MetadataType metadataType,
                                    Class<?> keyIdResolverDeclarationClass) {
    this.parameterName = parameterName;
    this.metadataType = metadataType;
    this.categoryName = categoryName;
    this.keyIdResolverDeclarationClass = keyIdResolverDeclarationClass;
  }

  @Override
  public boolean hasKeyIdResolver() {
    return (!NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass) &&
        !org.mule.runtime.extension.api.metadata.NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass)) ||
        ((metadataType != null) && ((metadataType instanceof BooleanType) || isEnum(metadataType)));
  }

  @Override
  public boolean isPartialKeyResolver() {
    return PartialTypeKeysResolver.class.isAssignableFrom(keyIdResolverDeclarationClass) ||
        org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver.class.isAssignableFrom(keyIdResolverDeclarationClass);
  }

  @Override
  public TypeKeysResolver getKeyResolver() {
    TypeKeysResolver typeKeysResolver;
    if (metadataType == null || categoryName == null) {
      typeKeysResolver = instantiateResolver();
    } else if (NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass) ||
        org.mule.runtime.extension.api.metadata.NullMetadataResolver.class.isAssignableFrom(keyIdResolverDeclarationClass)) {
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

  public Class<?> keyIdResolverDeclarationClass() {
    return keyIdResolverDeclarationClass;
  }

  @Override
  public MetadataType getMetadataType() {
    return metadataType;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  private TypeKeysResolver instantiateResolver() {
    try {
      Object resolver = ClassUtils.instantiateClass(keyIdResolverDeclarationClass);
      if (resolver instanceof org.mule.sdk.api.metadata.resolving.TypeKeysResolver) {
        return MuleTypeKeysResolverAdapter.from(resolver);
      } else {
        return (TypeKeysResolver) resolver;
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of type "
          + getClassName(keyIdResolverDeclarationClass)), e);
    }
  }

}
