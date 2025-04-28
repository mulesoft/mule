/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockDeprecatedAst;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockTypeLoader;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.setMockAstChild;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.singleParameterAst;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.model.ExtensionModelHelper;
import org.mule.runtime.ast.internal.model.DefaultExtensionModelHelper;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for building {@link MuleSdkParameterModelParser} instances for testing by mocking {@link ComponentAst}.
 */
class MuleSdkParameterModelParserSdkBuilder {

  private final Set<ExtensionDeclarer> extensionDeclarers = new HashSet<>();
  private final String name;
  private String type;
  private Map<String, MetadataType> typeLoaderTypes;
  private ComponentAst deprecatedAst;

  /**
   * Creates the parser builder with the mandatory parameters {@link #name} and {@link #type}.
   *
   * @param name The parameter name.
   * @param type The parameter type.
   */
  public MuleSdkParameterModelParserSdkBuilder(String name, String type) {
    this.name = name;
    this.type = type;
  }

  /**
   * Allows for overriding the type defined during the construction.
   *
   * @param type The parameter type.
   * @return This instance for chaining purposes.
   */
  public MuleSdkParameterModelParserSdkBuilder withType(String type) {
    this.type = type;
    return this;
  }

  /**
   * Defines the type mapping for the {@link TypeLoader} that the parameter parser will have access to.
   *
   * @param typeLoaderTypes The type mappings for the {@link TypeLoader}.
   * @return This instance for chaining purposes.
   */
  public MuleSdkParameterModelParserSdkBuilder withTypeLoaderTypes(Map<String, MetadataType> typeLoaderTypes) {
    this.typeLoaderTypes = typeLoaderTypes;
    return this;
  }

  /**
   * Allows for defining extension in context for the parameter parsing.
   * <p>
   * The given {@code extensionDeclarer} will be added to the set of {@link ExtensionDeclarer} which will later be loaded into
   * {@link ExtensionModel} before building the parameter parser. No loading is done until the parameter parser is built.
   *
   * @param extensionDeclarer The {@link ExtensionDeclarer} to add to the parsing context.
   * @return This instance for chaining purposes.
   */
  public MuleSdkParameterModelParserSdkBuilder withExtensionInContext(ExtensionDeclarer extensionDeclarer) {
    extensionDeclarers.add(extensionDeclarer);
    return this;
  }

  /**
   * Adds a deprecation model to the {@link ComponentAst} to be used when creating the parameter parser.
   *
   * @param since      String to return as the "since" parameter of the mock construct.
   * @param message    String to return as the "message" parameter of the mock construct.
   * @param toRemoveIn String to return as the "toRemoveIn" parameter of the mock construct.
   * @return This instance for chaining purposes.
   */
  public MuleSdkParameterModelParserSdkBuilder withDeprecationModel(String since, String message, String toRemoveIn) {
    deprecatedAst = mockDeprecatedAst(since, message, toRemoveIn);
    return this;
  }

  /**
   * Builds the {@link MuleSdkParameterModelParser} using all the defined characteristics.
   *
   * @return The {@link MuleSdkParameterModelParser}
   */
  public MuleSdkParameterModelParser build() {
    final TypeLoader typeLoader = mockTypeLoader(typeLoaderTypes != null ? typeLoaderTypes : emptyMap());
    final ExtensionModelHelper extensionModelHelper = new DefaultExtensionModelHelper(loadExtensionModels());

    final ComponentAst componentAst = mock(ComponentAst.class);

    final ComponentParameterAst parameterNameAst = singleParameterAst(name);
    when(componentAst.getParameter(DEFAULT_GROUP_NAME, "name")).thenReturn(parameterNameAst);

    final ComponentParameterAst typeParameterAst = singleParameterAst(type);
    when(componentAst.getParameter(DEFAULT_GROUP_NAME, "type")).thenReturn(typeParameterAst);

    if (deprecatedAst != null) {
      setMockAstChild(componentAst, "deprecated", deprecatedAst);
    }

    return new MuleSdkParameterModelParser(componentAst, typeLoader, extensionModelHelper);
  }

  private Set<ExtensionModel> loadExtensionModels() {
    if (extensionDeclarers.isEmpty()) {
      return emptySet();
    }

    return loadExtensionModels(createLoadingRequest());
  }

  private Set<ExtensionModel> loadExtensionModels(ExtensionModelLoadingRequest loadingRequest) {
    return extensionDeclarers.stream()
        .map(declarer -> new ExtensionModelLoader() {

          @Override
          public String getId() {
            return MuleSdkParameterModelParserSdkBuilder.class.getName() + "#" + declarer.getDeclaration().getName();
          }

          @Override
          protected void declareExtension(ExtensionLoadingContext context) {
            // nothing to do
          }
        }.loadExtensionModel(declarer, loadingRequest))
        .collect(toSet());
  }

  private ExtensionModelLoadingRequest createLoadingRequest() {
    return ExtensionModelLoadingRequest
        .builder(MuleSdkParameterModelParserSdkBuilder.class.getClassLoader(), nullDslResolvingContext())
        .build();
  }

}
