/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.getAliasName;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.introspection.declaration.type.annotation.ExtensibleTypeAnnotation;
import org.mule.runtime.extension.api.introspection.declaration.type.annotation.TypeAliasAnnotation;

import java.lang.reflect.Modifier;

/**
 * Set of utility operations to handle {@link MetadataType}
 *
 * @since 4.0
 */
public final class MetadataTypeUtils {

  private MetadataTypeUtils() {}

  public static boolean isNullType(MetadataType type) {
    return type instanceof NullType;
  }

  public static boolean isObjectType(MetadataType type) {
    return type instanceof ObjectType;
  }

  public static boolean isVoid(MetadataType type) {
    return isNullType(type);
  }

  /**
   * @param metadataType the {@link MetadataType} to inspect to retrieve its type Alias
   * @return the {@code Alias} name of the {@link MetadataType}
   */
  public static String getAlias(MetadataType metadataType) {
    return metadataType.getAnnotation(TypeAliasAnnotation.class).map(TypeAliasAnnotation::getValue)
        .orElse(metadataType.getMetadataFormat().equals(JAVA) ? getAliasName(getType(metadataType)) : "");
  }

  /**
   * @param metadataType the {@link MetadataType} to inspect to retrieve its type Alias
   * @param defaultName default name to use if {@code metadataType} alias is not defined
   * @return the {@code Alias} name of the {@link MetadataType} or the {@code defaultName} if alias was not specified
   */
  public static String getAlias(MetadataType metadataType, String defaultName) {
    return metadataType.getAnnotation(TypeAliasAnnotation.class).map(TypeAliasAnnotation::getValue)
        .orElse(metadataType.getMetadataFormat().equals(JAVA)
            ? getAliasName(defaultName, getType(metadataType).getAnnotation(Alias.class))
            : defaultName);
  }

  public static boolean isInstantiable(MetadataType metadataType) {
    try {
      return metadataType.getAnnotation(ClassInformationAnnotation.class).map(ClassInformationAnnotation::isInstantiable)
          .orElse(metadataType.getMetadataFormat().equals(JAVA) && IntrospectionUtils.isInstantiable(getType(metadataType)));
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean hasExposedFields(MetadataType metadataType) {
    return metadataType instanceof ObjectType && !((ObjectType) metadataType).getFields().isEmpty();
  }

  public static boolean isFinal(MetadataType metadataType) {
    try {
      return metadataType.getAnnotation(ClassInformationAnnotation.class).map(ClassInformationAnnotation::isFinal)
          .orElse(metadataType.getMetadataFormat().equals(JAVA) && Modifier.isFinal(getType(metadataType).getModifiers()));
    } catch (Exception e) {
      return false;
    }
  }

  public static String getId(MetadataType metadataType) {
    try {
      return org.mule.metadata.utils.MetadataTypeUtils.getTypeId(metadataType)
          .orElse(metadataType.getMetadataFormat().equals(JAVA) ? getType(metadataType).getName() : "");
    } catch (Exception e) {
      return "";
    }
  }

  public static boolean isExtensible(MetadataType metadataType) {
    return metadataType.getAnnotation(ExtensibleTypeAnnotation.class).isPresent();
  }

  public static boolean isEnum(MetadataType metadataType) {
    return metadataType.getAnnotation(EnumAnnotation.class).isPresent();
  }

}
