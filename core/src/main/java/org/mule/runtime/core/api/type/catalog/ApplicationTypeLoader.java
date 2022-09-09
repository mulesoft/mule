/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.meta.type.TypeCatalog.getDefault;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.type.TypeCatalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * a {@link TypeLoader} for obtaining types available in the context of the current application. It accepts primitive type names
 * (such as string, number, etc.) and can also access types defined in other extensions by using the
 * {@code <extension_namespace>:<type_name>} syntax.
 *
 * @since 4.5.0
 */
public class ApplicationTypeLoader implements TypeLoader {

  private final TypeLoader primitivesTypeLoader = new PrimitiveTypesTypeLoader();
  private final TypeLoader specialTypesLoader = new SpecialTypesTypeLoader();
  private final Map<String, String> extensionModelNamesByPrefix;
  private final TypeCatalog dependenciesTypeCatalog;

  public ApplicationTypeLoader(Set<ExtensionModel> extensionModels) {
    this.dependenciesTypeCatalog = getDefault(extensionModels);

    extensionModelNamesByPrefix = new HashMap<>(extensionModels.size());
    for (ExtensionModel extensionModel : extensionModels) {
      extensionModelNamesByPrefix.put(extensionModel.getXmlDslModel().getPrefix(), extensionModel.getName());
    }
  }

  @Override
  public Optional<MetadataType> load(String typeIdentifier) {
    Optional<MetadataType> primitive = primitivesTypeLoader.load(typeIdentifier);
    if (primitive.isPresent()) {
      return primitive;
    }

    Optional<MetadataType> special = specialTypesLoader.load(typeIdentifier);
    if (special.isPresent()) {
      return special;
    }

    return getFromDependency(typeIdentifier);
  }

  // The string format can be the full name of the type, or a string with syntax <extension-prefix>:<type-alias> if
  // the type has an alias. This format is temporal in order to use the types from the test operations, but may change
  // when the type catalog using dataweave is fully implemented.
  // TODO (W-11706194 and W-11706243): Adapt the syntax when the type resolution is delegated to DW.
  private Optional<MetadataType> getFromDependency(String typeIdentifier) {
    ComponentIdentifier componentIdentifier = buildFromStringRepresentation(typeIdentifier);
    String extensionName = extensionModelNamesByPrefix.get(componentIdentifier.getNamespace());
    String typeAlias = componentIdentifier.getName();
    Collection<ObjectType> typesFromExtension = dependenciesTypeCatalog.getExtensionTypes(extensionName);
    for (ObjectType objectType : typesFromExtension) {
      if (matchesAlias(typeAlias, objectType)) {
        return of(objectType);
      }
    }

    Optional<ObjectType> typeByFullName = dependenciesTypeCatalog.getType(typeIdentifier);
    if (typeByFullName.isPresent()) {
      return of(typeByFullName.get());
    }

    return empty();
  }

  private static boolean matchesAlias(String expectedAlias, MetadataType metadataType) {
    return metadataType.getAnnotation(TypeAliasAnnotation.class)
        .map(typeAliasAnnotation -> typeAliasAnnotation.getValue().equals(expectedAlias)).orElse(false);
  }
}
