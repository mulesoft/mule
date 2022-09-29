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
import static java.util.stream.Collectors.toSet;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader;
import org.mule.metadata.message.api.MuleEventMetadataTypeBuilder;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * a {@link TypeLoader} for obtaining types available in the context of the current application. It accepts primitive type names
 * (such as string, number, etc.) and can also access types defined in other extensions by using the
 * {@code <extension_namespace>:<type_name>} syntax.
 *
 * @since 4.5.0
 */
public class ApplicationTypeLoader implements TypeLoader {

  private final Set<ModuleDefinition> moduleDefinitions;
  private final MockExpressionLanguageServiceImpl elms;

  // TODO: Remove this constructor and migrate usages to the other one
  public ApplicationTypeLoader(Set<ExtensionModel> extensionModels) {
    this(extensionModels, new MockExpressionLanguageServiceImpl(extensionModels));
  }

  // TODO: Migrate the MockExpressionLanguageServiceImpl to ExpressionLanguageMetadataService
  // TODO: Change the param name!
  public ApplicationTypeLoader(Set<ExtensionModel> extensionModels, MockExpressionLanguageServiceImpl elms) {
    Function<ExtensionModel, ModuleDefinition> toModule = new ExtensionModelToModuleDefinitionTransformer();
    moduleDefinitions = extensionModels.stream().map(toModule).collect(toSet());
    this.elms = elms;
  }

  @Override
  public Optional<MetadataType> load(String typeIdentifier) {
    return elms.resolveType(typeIdentifier, moduleDefinitions);
  }

  // TODO: Remove this class
  private static class MockExpressionLanguageServiceImpl implements ExpressionLanguageMetadataService {

    private final TypeLoader primitivesTypeLoader = new PrimitiveTypesTypeLoader();
    private final TypeLoader specialTypesLoader = new SpecialTypesTypeLoader();
    private final Map<String, String> extensionModelNamesByPrefix;
    private final TypeCatalog dependenciesTypeCatalog;

    MockExpressionLanguageServiceImpl(Set<ExtensionModel> extensionModels) {
      this.dependenciesTypeCatalog = getDefault(extensionModels);

      extensionModelNamesByPrefix = new HashMap<>(extensionModels.size());
      for (ExtensionModel extensionModel : extensionModels) {
        extensionModelNamesByPrefix.put(extensionModel.getXmlDslModel().getPrefix(), extensionModel.getName());
      }
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public void getInputType(String expression, MetadataType output, MuleEventMetadataTypeBuilder builder,
                             MessageCallback callback) {

    }

    @Override
    public MetadataType getOutputType(TypeBindings typeBindings, String expression, MessageCallback callback) {
      return null;
    }

    @Override
    public MetadataType getOutputType(TypeBindings typeBindings, String expression, String outputMimeType,
                                      MessageCallback callback) {
      return null;
    }

    @Override
    public MetadataType getMetadataFromSample(InputStream sample, Map<String, Object> readerProperties, String mimeType) {
      return null;
    }

    @Override
    public boolean isAssignable(MetadataType assignment, MetadataType expected, MessageCallback callback) {
      return false;
    }

    @Override
    public Map<String, MetadataType> resolveAssignment(MetadataType assignment, MetadataType expected, MessageCallback callback) {
      return null;
    }

    @Override
    public MetadataType substitute(MetadataType assignment, Map<String, MetadataType> substitution) {
      return null;
    }

    @Override
    public MetadataType unify(List<MetadataType> metadataTypes) {
      return null;
    }

    @Override
    public MetadataType intersect(List<MetadataType> metadataTypes) {
      return null;
    }

    @Override
    public MetadataTypeSerializer getTypeSerializer() {
      return null;
    }

    @Override
    public TypeLoader createTypeLoader(String content, MetadataFormat metadataFormat) {
      return null;
    }

    @Override
    public ModuleDefinition moduleDefinition(String nameIdentifier, Collection<ModuleDefinition> modules)
        throws ExpressionCompilationException {
      return null;
    }

    public Optional<MetadataType> resolveType(String typeIdentifier, Collection<ModuleDefinition> moduleDefinitions) {
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
}
