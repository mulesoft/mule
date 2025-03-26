/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.type.catalog;


import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ArtifactTypeLoader;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.core.api.extension.ExtensionManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.slf4j.Logger;

/**
 * A {@link ArtifactTypeLoader} for obtaining types available in the context of the current artifact. It accepts primitive type
 * names (such as string, number, etc.) and can also access types defined in other extensions by using the
 * {@code <extension_namespace>:<type_name>} syntax.
 * <p>
 * Furthermore, if a type cannot be obtained with the mentioned syntax, this implementation will delegate the type resolution to
 * the {@link ExpressionLanguageMetadataService}.
 *
 * @since 4.5.0
 */
public class DefaultArtifactTypeLoader implements ArtifactTypeLoader, Initialisable {

  private static final Logger LOGGER = getLogger(DefaultArtifactTypeLoader.class);

  private final TypeLoader primitivesTypeLoader = new PrimitiveTypesTypeLoader();
  private final TypeLoader specialTypesLoader = new SpecialTypesTypeLoader();

  private Map<String, Collection<ObjectType>> typesByExtension;
  private Map<String, Optional<MetadataType>> loadedTypes;
  private ExpressionLanguageMetadataService expressionLanguageMetadataService;
  private Collection<ExtensionModel> extensionModels;
  private Collection<ModuleDefinition> moduleDefinitions;

  public DefaultArtifactTypeLoader() {
    // This constructor is intended to be used in some context such that the dependency injection will happen.
  }

  public DefaultArtifactTypeLoader(Collection<ExtensionModel> extensionModels,
                                   ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    requireNonNull(extensionModels, "ExtensionModels collection cannot be null.");
    this.extensionModels = extensionModels;
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
  }

  @Inject
  public void setExtensionManager(ExtensionManager extensionManager) {
    this.extensionModels = extensionManager.getExtensions();
  }

  @Inject
  public void setExpressionLanguageMetadataService(ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (extensionModels == null) {
      extensionModels = emptySet();
      LOGGER.warn("DefaultArtifactTypeLoader has been initialized with a null Collection of ExtensionModels");
    }
    typesByExtension = new HashMap<>();
    loadedTypes = new ConcurrentHashMap<>();
    for (ExtensionModel extensionModel : extensionModels) {
      String extensionPrefix = extensionModel.getXmlDslModel().getPrefix();
      typesByExtension.put(extensionPrefix, extensionModel.getTypes());
    }
    this.moduleDefinitions = extensionModelsToModuleDefinitions(extensionModels);
  }

  private static Collection<ModuleDefinition> extensionModelsToModuleDefinitions(Collection<ExtensionModel> extensionModels) {
    Function<ExtensionModel, ModuleDefinition> toModule = new ExtensionModelToModuleDefinitionTransformer();
    return extensionModels.stream().map(toModule).collect(toSet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataType> load(String typeIdentifier) {
    return loadedTypes.computeIfAbsent(typeIdentifier, this::doLoad);
  }

  private Optional<MetadataType> doLoad(String typeIdentifier) {
    Optional<MetadataType> primitive = primitivesTypeLoader.load(typeIdentifier);
    if (primitive.isPresent()) {
      return primitive;
    }

    Optional<MetadataType> special = specialTypesLoader.load(typeIdentifier);
    if (special.isPresent()) {
      return special;
    }

    Optional<MetadataType> parsed = parseType(typeIdentifier);
    if (parsed.isPresent()) {
      return parsed;
    }

    return loadFromExpressionLanguageMetadataService(typeIdentifier);
  }

  private Optional<MetadataType> loadFromExpressionLanguageMetadataService(String typeExpression) {
    if (expressionLanguageMetadataService == null) {
      LOGGER
          .warn("Couldn't evaluate type expression '{}' because the ExpressionLanguageMetadataService wasn't correctly injected",
                typeExpression);
      return empty();
    }
    try {
      return ofNullable(expressionLanguageMetadataService.evaluateTypeExpression(typeExpression, moduleDefinitions));
    } catch (ExpressionCompilationException exception) {
      LOGGER.error("Failed to evaluate type expression '{}'", typeExpression, exception);
      return empty();
    }
  }

  private Optional<MetadataType> parseType(String typeIdentifier) {
    if (!typeIdentifier.contains(":")) {
      return empty();
    }
    // The first appearance of the separator and the String.substring methods are used instead of String.split because the type id
    // or alias might include the separator inside it.
    int separatorIndex = typeIdentifier.indexOf(":");
    String extensionIdentifier = typeIdentifier.substring(0, separatorIndex);
    String typeIdOrAlias = typeIdentifier.substring(separatorIndex + 1);

    if (typesByExtension.containsKey(extensionIdentifier)) {
      Set<MetadataType> typesWithTypeIdentifierAsAlias = new HashSet<>();
      for (MetadataType extensionType : typesByExtension.get(extensionIdentifier)) {
        Optional<String> extensionTypeTypeId = getTypeId(extensionType);
        if (extensionTypeTypeId.isPresent() && extensionTypeTypeId.get().equals(typeIdOrAlias)) {
          return of(extensionType);
        }
        String extensionTypeAlias = getAlias(extensionType);
        if (extensionTypeAlias != null && extensionTypeAlias.equals(typeIdOrAlias)) {
          typesWithTypeIdentifierAsAlias.add(extensionType);
        }
      }
      if (typesWithTypeIdentifierAsAlias.size() == 1) {
        return of(typesWithTypeIdentifierAsAlias.iterator().next());
      } else if (typesWithTypeIdentifierAsAlias.size() > 1) {
        throw new IllegalArgumentException(format("No type with identifier [%s] and more that one with that alias. Use typeId to remove ambiguity [%s]",
                                                  typeIdOrAlias, typesWithTypeIdentifierAsAlias.stream()
                                                      .map(metadataType -> getTypeId(metadataType).orElse(""))
                                                      .collect(joining(", "))));
      }
    }
    return empty();
  }
}
