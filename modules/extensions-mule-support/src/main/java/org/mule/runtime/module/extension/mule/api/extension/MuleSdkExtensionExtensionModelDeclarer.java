/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.extension;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.java.api.JavaTypeLoader.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.extension.api.annotation.Extension.MULESOFT;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_NAMESPACE;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_NAMESPACE_URI;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.mule.internal.dsl.processor.xml.MuleSdkExtensionDslNamespaceInfoProvider.MULE_EXTENSION_DSL_XSD_FILE_NAME;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;

import java.util.Arrays;

/**
 * An {@link ExtensionDeclarer} for Mule SDK Extensions
 *
 * @since 4.5
 */
public class MuleSdkExtensionExtensionModelDeclarer {

  public static final String EXTENSION_CONSTRUCT_NAME = "extension";

  public ExtensionDeclarer declareExtensionModel() {
    final BaseTypeBuilder typeBuilder = create(JAVA);

    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer()
        .named("Mule Extension DSL")
        .describedAs("DSL for declaring Mule SDK Extensions")
        .onVersion(MULE_VERSION)
        .fromVendor(MULESOFT_VENDOR)
        .withCategory(COMMUNITY)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(MULE_EXTENSION_DSL_NAMESPACE)
            .setNamespace(MULE_EXTENSION_DSL_NAMESPACE_URI)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName(MULE_EXTENSION_DSL_XSD_FILE_NAME)
            .setSchemaLocation(MULE_EXTENSION_DSL_SCHEMA_LOCATION)
            .build());

    declareExtensionConstruct(extensionDeclarer, typeBuilder);

    return extensionDeclarer;
  }

  private void declareExtensionConstruct(ExtensionDeclarer extensionDeclarer, BaseTypeBuilder typeBuilder) {
    ConstructDeclarer extensionConstruct = extensionDeclarer.withConstruct(EXTENSION_CONSTRUCT_NAME)
        .describedAs("Root element of an extension that contains configurations, connections, operations, sources and functions as children.")
        .allowingTopLevelDefinition();

    final ParameterGroupDeclarer<?> params = extensionConstruct.onDefaultParameterGroup();
    params.withRequiredParameter("name")
        .describedAs("Name of the extension that identifies it.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .asComponentId();

    String[] validCategories = Arrays.stream(Category.values()).map(Enum::name).toArray(String[]::new);
    params.withOptionalParameter("category")
        .describedAs("Category of the extension.")
        .ofType(typeBuilder.stringType().enumOf(validCategories).build())
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo(COMMUNITY.name());

    params.withOptionalParameter("vendor")
        .describedAs("Vendor of the extension.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo(MULESOFT);

    params.withOptionalParameter("requiredEntitlement")
        .describedAs("The required entitlement in the customer extension license.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("requiresEnterpriseLicense")
        .describedAs("If the extension requires an enterprise license to run.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("allowsEvaluationLicense")
        .describedAs("If the extension can be run with an evaluation license.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("namespace")
        .describedAs("Expected namespace of the extension to look for when generating the schemas. If left empty it will " +
            "default to http://www.mulesoft.org/schema/mule/[prefix], where [prefix] is the attribute prefix attribute value.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("prefix")
        .describedAs("Expected prefix of the extension to look for when generating the schemas. If left empty it will create a " +
            "default one based on the extension's name, removing the words \"extension\", \"module\" or \"connector\" at " +
            "the end if they are present and hyphenizing the resulting name.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }
}
