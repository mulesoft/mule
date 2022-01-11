/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.ast.api.util.MuleAstUtils.recursiveStreamWithHierarchy;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;

import org.mule.metadata.api.ClassTypeLoader;
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
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.DslConstants;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.input.ReaderInputStream;

public abstract class AbstractCoreValidationTestCase {

  protected static AstXmlParser parser;

  static {
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
        .withExtensionModel(MuleExtensionModelProvider.getExtensionModel())
        .withExtensionModel(new ExtensionModelFactory()
            .create(new DefaultExtensionLoadingContext(extensionDeclarer,
                                                       builder(AbstractCoreValidationTestCase.class.getClassLoader(),
                                                               new NullDslResolvingContext()).build())))
        .withSchemaValidationsDisabled()
        .build();
  }

  protected List<ValidationResultItem> runValidation(final String... xmlConfigs) {
    final List<Pair<String, InputStream>> configs = new ArrayList<>();

    for (int i = 0; i < xmlConfigs.length; i++) {
      configs.add(new Pair<>("test" + i, new ReaderInputStream(new StringReader(xmlConfigs[i]), UTF_8)));
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
