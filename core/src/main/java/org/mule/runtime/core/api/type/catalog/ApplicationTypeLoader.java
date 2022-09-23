/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static org.mule.runtime.api.util.IdentifierParsingUtils.parseComponentIdentifier;
import static org.mule.runtime.core.api.type.catalog.DefaultListableTypeLoadersRepository.from;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.util.Optional.empty;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ListableTypeLoadersRepository;

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

  private final ListableTypeLoadersRepository dependenciesTypeLoaderRepository;

  public ApplicationTypeLoader(Set<ExtensionModel> extensionModels) {
    dependenciesTypeLoaderRepository = from(extensionModels);
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
    try {
      ComponentIdentifier componentIdentifier = parseComponentIdentifier(typeIdentifier, CORE_PREFIX);
      String namespace = componentIdentifier.getNamespace();
      String typeIdOrAlias = componentIdentifier.getName();
      return dependenciesTypeLoaderRepository.getTypeLoaderByPrefix(namespace).load(typeIdOrAlias);
    } catch (MuleException e) {
      return empty();
    }
  }
}
