/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation.test;

import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.ast.api.util.MuleAstUtils.recursiveStreamWithHierarchy;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.extension.ComponentConfigurerTestUtils.createMockedFactory;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.internal.dsl.utils.DslConstants;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

import org.apache.commons.io.input.ReaderInputStream;

import org.junit.Before;

public abstract class AbstractCoreValidationTestCase {

  protected static AstXmlParser parser;

  @Before
  public void createAstXmlParser() {
    // Avoid recreating the same parser
    if (parser != null) {
      return;
    }

    final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault()
        .createTypeLoader(AbstractCoreValidationTestCase.class.getClassLoader());

    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer()
        .named("test")
        .describedAs("test")
        .onVersion("1.0.0")
        .fromVendor(MULESOFT_VENDOR)
        .withCategory(COMMUNITY)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix("test")
            .setNamespace(format(DslConstants.DEFAULT_NAMESPACE_URI_MASK, "test"))
            .setSchemaVersion("1.0.0")
            .setXsdFileName("test.xsd")
            .setSchemaLocation(format("%s/%s/%s.xsd", format(DslConstants.DEFAULT_NAMESPACE_URI_MASK, "test"), "current", "test"))
            .build());

    final ConfigurationDeclarer config = extensionDeclarer.withConfig("config");
    final ConfigurationDeclarer otherConfig = extensionDeclarer.withConfig("otherConfig");
    otherConfig.onDefaultParameterGroup().withRequiredParameter("count").ofType(ExtensionsTypeLoaderFactory.getDefault()
        .createTypeLoader(MuleExtensionModelProvider.class.getClassLoader()).load(Integer.class));

    final OperationDeclarer operation = extensionDeclarer.withOperation("operation");
    operation.withOutput().ofType(typeLoader.load(void.class));
    operation.withOutputAttributes().ofType(typeLoader.load(void.class));

    parser = AstXmlParser.builder()
        .withExtensionModels(resolveRuntimeExtensionModels())
        .withExtensionModel(new ExtensionModelLoader() {

          @Override
          public String getId() {
            return AbstractCoreValidationTestCase.class.getName();
          }

          @Override
          protected void declareExtension(ExtensionLoadingContext context) {
            // nothing to do
          }
        }.loadExtensionModel(extensionDeclarer, builder(AbstractCoreValidationTestCase.class.getClassLoader(),
                                                        nullDslResolvingContext())
            .build()))
        .withSchemaValidationsDisabled()
        .build();
  }

  protected List<ExtensionModel> resolveRuntimeExtensionModels() {
    MuleExtensionModelProvider.setConfigurerFactory(createMockedFactory());
    return asList(MuleExtensionModelProvider.getExtensionModel());
  }

  protected List<ValidationResultItem> runValidation(String configFileNamePrefix, final String... xmlConfigs) {
    final List<Pair<String, InputStream>> configs = new ArrayList<>();

    for (int i = 0; i < xmlConfigs.length; i++) {
      configs.add(new Pair<>(configFileNamePrefix + i, new ReaderInputStream(new StringReader(xmlConfigs[i]), UTF_8)));
    }

    final ArtifactAst ast = parser.parse(configs);

    return recursiveStreamWithHierarchy(ast)
        .filter(c -> getValidation().applicable()
            .test(ImmutableList.<ComponentAst>builder().addAll(c.getSecond()).add(c.getFirst()).build()))
        .flatMap(c -> getValidation().validateMany(c.getFirst(), ast).stream())
        .collect(toList());
  }

  protected abstract Validation getValidation();
}
