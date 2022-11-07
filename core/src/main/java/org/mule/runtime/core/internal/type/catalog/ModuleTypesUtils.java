/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.type.catalog;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.runtime.api.meta.model.ExtensionModel;

/**
 * Util class to retrieve a {@link MetadataType} from {@link ExtensionModel}
 * 
 * @since 4.5
 */
public class ModuleTypesUtils {

  /**
   * Retrieves a {@link MetadataType}
   *
   *
   * @param moduleDefinitions the list of {@link ModuleDefinition} that may contain the {@link MetadataType}.
   * @param moduleName        the identifier for the module where the {@link MetadataType} is defined.
   * @param typeIdentifier    either the typeId or the typeAlias for the {@link MetadataType}.
   *
   * @return the {@link MetadataType}
   */
  public static MetadataType getType(Collection<ModuleDefinition> moduleDefinitions,
                                     String moduleName, String typeIdentifier) {
    ModuleDefinition moduleDefinition = getModuleDefinition(moduleDefinitions, moduleName);
    Set<MetadataType> hasTypeIdentifierAsAlias = new HashSet<>();
    for (MetadataType moduleType : moduleDefinition.declaredTypes()) {
      Optional<String> moduleTypeTypeTypeId = getTypeId(moduleType);
      if (moduleTypeTypeTypeId.isPresent() && moduleTypeTypeTypeId.get().equals(typeIdentifier)) {
        return moduleType;
      }
      String moduleTypeAlias = getAlias(moduleType);
      if (moduleTypeAlias != null && moduleTypeAlias.equals(typeIdentifier)) {
        hasTypeIdentifierAsAlias.add(moduleType);
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
                                              moduleName));
  }

  private static ModuleDefinition getModuleDefinition(Collection<ModuleDefinition> moduleDefinitions,
                                                      String moduleName) {
    return moduleDefinitions.stream()
        .filter(moduleDefinition -> moduleDefinition.getName().getElements()[0].equals(moduleName)).findAny()
        .orElseThrow(() -> new IllegalArgumentException(format("No extension found with identifier [%s]", moduleName)));
  }

}
