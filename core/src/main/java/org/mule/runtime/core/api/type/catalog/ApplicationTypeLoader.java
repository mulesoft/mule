/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static java.util.stream.Collectors.toSet;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.core.internal.type.catalog.ExtensionModelToModuleDefinitionTransformer;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

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

  private final Collection<ModuleDefinition> moduleDefinitions;
  private final ExpressionLanguageMetadataService expressionLanguageMetadataService;

  public ApplicationTypeLoader(Collection<ExtensionModel> extensionModels,
                               ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.moduleDefinitions = extensionModelsToModuleDefinitions(extensionModels);
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
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

    return expressionLanguageMetadataService.evaluateTypeExpression(typeIdentifier, moduleDefinitions);
  }

  private static Collection<ModuleDefinition> extensionModelsToModuleDefinitions(Collection<ExtensionModel> extensionModels) {
    Function<ExtensionModel, ModuleDefinition> toModule = new ExtensionModelToModuleDefinitionTransformer();
    return extensionModels.stream().map(toModule).collect(toSet());
  }
}
