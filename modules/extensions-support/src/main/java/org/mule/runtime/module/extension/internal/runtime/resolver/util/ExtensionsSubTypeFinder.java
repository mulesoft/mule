/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver.util;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;

/**
 * Util class to retrieve a {@link MetadataType} described by a {@link org.mule.runtime.api.meta.model.SubTypesModel}
 * 
 * @since 4.5
 */
public class ExtensionsSubTypeFinder {

  /**
   * Retrieves a {@link MetadataType}
   *
   *
   * @param extensionModels     the list of {@link ExtensionModel} that may contain the {@link MetadataType}.
   * @param extensionIdentifier the identifier for the extension where the {@link org.mule.runtime.api.meta.model.SubTypesModel}
   *                            is defined.
   * @param typeIdentifier      either the typeId or the typeAlias for the {@link MetadataType}.
   * @param baseType            the base type the actual {@link MetadataType} must be subtyped.
   *
   * @return the {@link MetadataType}
   */
  public static MetadataType getSubtype(Collection<ExtensionModel> extensionModels,
                                        String extensionIdentifier, String typeIdentifier, MetadataType baseType) {
    ExtensionModel extensionModel = getExtensionModel(extensionModels, extensionIdentifier);
    Set<MetadataType> hasTypeIdentifierAsAlias = new HashSet<>();
    for (MetadataType extensionType : extensionModel.getTypes()) {
      Optional<String> extensionTypeTypeId = getTypeId(extensionType);
      if (extensionTypeTypeId.isPresent() && extensionTypeTypeId.get().equals(typeIdentifier)) {
        checkIsSubType(extensionModel, baseType, extensionType);
        return extensionType;
      }
      String extensionTypeAlias = getAlias(extensionType);
      if (extensionTypeAlias != null && extensionTypeAlias.equals(typeIdentifier)
          && isSubType(extensionModel, baseType, extensionType)) {
        hasTypeIdentifierAsAlias.add(extensionType);
      }
    }
    if (hasTypeIdentifierAsAlias.size() == 1) {
      return hasTypeIdentifierAsAlias.iterator().next();
    } else if (hasTypeIdentifierAsAlias.size() > 1) {
      throw new IllegalArgumentException(format("No type with identifier [%s] and more that one with that alias. Use typeId to remove ambiguity [%s]",
                                                typeIdentifier, hasTypeIdentifierAsAlias.stream()
                                                    .map(metadataType -> getTypeId(metadataType).orElse(""))
                                                    .collect(joining(", "))));
    }
    throw new IllegalArgumentException(format("No type with identifier [%s] was found for extension [%s]", typeIdentifier,
                                              extensionIdentifier));
  }

  private static void checkIsSubType(ExtensionModel extensionModel, MetadataType baseType, MetadataType subType) {
    if (!isSubType(extensionModel, baseType, subType)) {
      throw new IllegalArgumentException(format("Value of type [%s] cannot be applied to parameter/field of type [%s]",
                                                getTypeId(subType).orElse(""), getTypeId(baseType).orElse("")));
    }
  }

  private static boolean isSubType(ExtensionModel extensionModel, MetadataType baseType, MetadataType subType) {
    return extensionModel.getSubTypes().stream()
        .anyMatch(subTypesModel -> subTypesModel.getBaseType().equals(baseType) && subTypesModel.getSubTypes().contains(subType));
  }


  private static ExtensionModel getExtensionModel(Collection<ExtensionModel> extensionModels, String extensionIdentifier) {
    return extensionModels.stream()
        .filter(extensionModel -> extensionModel.getXmlDslModel().getPrefix().equals(extensionIdentifier)).findAny()
        .orElseThrow(() -> new IllegalArgumentException(format("No extension found with identifier [%s]", extensionIdentifier)));
  }

}
