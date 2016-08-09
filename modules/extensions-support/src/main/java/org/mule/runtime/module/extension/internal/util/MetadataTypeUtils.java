/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.introspection.declaration.type.annotation.ExtensibleTypeAnnotation;
import org.mule.runtime.extension.api.util.SubTypesMappingContainer;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.Modifier;
import java.util.List;

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
  public static String getAliasName(MetadataType metadataType) {
    return IntrospectionUtils.getAliasName(getType(metadataType));
  }

  /**
   * @param metadataType the {@link MetadataType} to inspect to retrieve its type Alias
   * @param defaultName default name to use if {@code metadataType} alias is not defined
   * @return the {@code Alias} name of the {@link MetadataType} or the {@code defaultName} if alias was not specified
   */
  public static String getAliasName(MetadataType metadataType, String defaultName) {
    Class<?> type = getType(metadataType);
    return IntrospectionUtils.getAliasName(defaultName, type.getAnnotation(Alias.class));
  }

  /**
   * Provides a unique way to generate an {@link UnionType} from the SubTypes Mapping declaration for a given {@link MetadataType
   * baseType}.
   * <p>
   * If no subType mapping exists for the given {@link MetadataType baseType}, then the {@code baseType} is returned, without
   * wrapping it in a new {@link UnionType}.
   * <p>
   * When there is a single subtype mapped, then instead of returning an {@link UnionType} of a single {@link MetadataType}, the
   * subtype is returned.
   * <p>
   * In all cases, if the {@link MetadataType baseType} is a concrete implementation, it is also added as a part of the
   * {@link UnionType}.
   *
   * @param baseType the base {@link MetadataType} for which subtypes could be mapped
   * @param subtypesContainer the {@link SubTypesMappingContainer} used to look for mapped subtypes for the given {@code baseType}
   * @return The {@code baseType} if no subtypes were present, its subtype if only one mapping is defined, or the {@link UnionType
   *         union} of all the mapped subtypes for the given {@code baseType}
   */
  public static MetadataType subTypesUnion(MetadataType baseType, SubTypesMappingContainer subtypesContainer,
                                           ClassLoader classLoader) {
    List<MetadataType> subTypes = subtypesContainer.getSubTypes(baseType);
    if (subTypes.isEmpty()) {
      return baseType;
    }

    boolean baseIsInstantiable = isInstantiable(baseType);
    if (subTypes.size() == 1 && !baseIsInstantiable) {
      // avoid single type union
      return subTypes.get(0);
    }

    ImmutableList.Builder<MetadataType> union = ImmutableList.<MetadataType>builder().addAll(subTypes);
    if (baseIsInstantiable) {
      union.add(baseType);
    }

    UnionTypeBuilder<?> unionTypeBuilder = BaseTypeBuilder.create(baseType.getMetadataFormat()).unionType();
    union.build().forEach(unionTypeBuilder::of);
    return unionTypeBuilder.build();
  }

  public static boolean isInstantiable(MetadataType metadataType) {
    return metadataType.getAnnotation(ClassInformationAnnotation.class).map(ClassInformationAnnotation::isInstantiable)
        .orElse(metadataType.getMetadataFormat().equals(MetadataFormat.JAVA)
            && IntrospectionUtils.isInstantiable(getType(metadataType)));
  }

  public static boolean hasExposedFields(MetadataType metadataType) {
    return metadataType instanceof ObjectType && !((ObjectType) metadataType).getFields().isEmpty();
  }

  public static boolean isFinal(MetadataType metadataType) {
    return metadataType.getAnnotation(ClassInformationAnnotation.class).map(ClassInformationAnnotation::isFinal)
        .orElse(metadataType.getMetadataFormat().equals(MetadataFormat.JAVA)
            && Modifier.isFinal(getType(metadataType).getModifiers()));
  }

  public static String getId(MetadataType metadataType) {
    return org.mule.metadata.utils.MetadataTypeUtils.getTypeId(metadataType)
        .orElse(metadataType.getMetadataFormat().equals(MetadataFormat.JAVA) ? getType(metadataType).getName() : "");
  }

  public static boolean isExtensible(MetadataType metadataType) {
    return metadataType.getAnnotation(ExtensibleTypeAnnotation.class).isPresent();
  }

  public static boolean isEnum(MetadataType metadataType) {
    return metadataType.getAnnotation(EnumAnnotation.class).isPresent();
  }
}
