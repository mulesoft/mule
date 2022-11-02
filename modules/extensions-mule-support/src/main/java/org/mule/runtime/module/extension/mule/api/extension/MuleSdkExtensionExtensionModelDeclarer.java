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
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_ALLOWS_EVALUATION_LICENSE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_CATEGORY_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DESCRIPTION_COMPONENT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_ERROR_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_LICENSING_COMPONENT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_NAMESPACE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_NAME_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_PREFIX_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_REQUIRED_ENTITLEMENT_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_REQUIRES_ENTERPRISE_LICENSE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_VENDOR_PARAMETER_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_NAMESPACE;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_NAMESPACE_URI;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_SCHEMA_LOCATION;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_XSD_FILE_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_XML_DSL_ATTRIBUTES_COMPONENT_NAME;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;

import java.util.Arrays;

/**
 * An {@link ExtensionDeclarer} for Mule SDK Extensions
 *
 * @since 4.5
 */
public class MuleSdkExtensionExtensionModelDeclarer {

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
            .setPrefix(MULE_SDK_EXTENSION_DSL_NAMESPACE)
            .setNamespace(MULE_SDK_EXTENSION_DSL_NAMESPACE_URI)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName(MULE_SDK_EXTENSION_DSL_XSD_FILE_NAME)
            .setSchemaLocation(MULE_SDK_EXTENSION_DSL_SCHEMA_LOCATION)
            .build());

    declareExtensionConstruct(extensionDeclarer, typeBuilder);

    return extensionDeclarer;
  }

  private void declareExtensionConstruct(ExtensionDeclarer extensionDeclarer, BaseTypeBuilder typeBuilder) {
    ConstructDeclarer extensionConstruct = extensionDeclarer.withConstruct(MULE_SDK_EXTENSION_CONSTRUCT_NAME)
        .describedAs("Root element of an extension that contains configurations, connections, operations, sources and functions as children.")
        .allowingTopLevelDefinition();

    declareDescriptionComponent(extensionConstruct, typeBuilder);
    declareErrorsComponent(extensionConstruct);
  }

  private void declareErrorsComponent(ConstructDeclarer extensionDeclarer) {
    NestedComponentDeclarer<?, ?> errorsDef = extensionDeclarer.withOptionalComponent(MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME)
        .describedAs("Top level element of an extension that contains the errors that the extension's operations are able to raise.")
        .withMinOccurs(0)
        .withMaxOccurs(1);

    NestedComponentDeclarer<?, ?> eachErrorDef = errorsDef.withOptionalComponent(MULE_SDK_EXTENSION_DSL_ERROR_CONSTRUCT_NAME)
        .describedAs("Declares an error, and may define the parent.")
        .withMinOccurs(0);

    ParameterGroupDeclarer<?> eachErrorParams = eachErrorDef.onDefaultParameterGroup();
    eachErrorParams.withRequiredParameter("type")
        .describedAs("Type of the error being declared")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
    eachErrorParams.withOptionalParameter("parent")
        .describedAs("Parent of the error being declared")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareDescriptionComponent(ConstructDeclarer extensionDeclarer, BaseTypeBuilder typeBuilder) {
    NestedComponentDeclarer<?, ?> descriptionDef = extensionDeclarer.withComponent(MULE_SDK_EXTENSION_DESCRIPTION_COMPONENT_NAME)
        .describedAs("Top level element of an extension that contains descriptive information about it.")
        .withMinOccurs(1)
        .withMaxOccurs(1);

    final ParameterGroupDeclarer<?> params = descriptionDef.onDefaultParameterGroup();
    params.withRequiredParameter(MULE_SDK_EXTENSION_NAME_PARAMETER_NAME)
        .describedAs("Name of the extension that identifies it.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .asComponentId();

    String[] validCategories = Arrays.stream(Category.values()).map(Enum::name).toArray(String[]::new);
    params.withOptionalParameter(MULE_SDK_EXTENSION_CATEGORY_PARAMETER_NAME)
        .describedAs("Category of the extension.")
        .ofType(typeBuilder.stringType().enumOf(validCategories).build())
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo(COMMUNITY.name());

    params.withOptionalParameter(MULE_SDK_EXTENSION_VENDOR_PARAMETER_NAME)
        .describedAs("Vendor of the extension.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo(MULESOFT);

    declareLicensingComponent(descriptionDef);
    declareXmlDslAttributesComponent(descriptionDef);
  }

  private void declareLicensingComponent(ComponentDeclarer<?, ?> def) {
    NestedComponentDeclarer<?, ?> licensingDef = def.withComponent(MULE_SDK_EXTENSION_LICENSING_COMPONENT_NAME)
        .describedAs("Child element of the extension's description that contains licensing information.")
        .withMinOccurs(0)
        .withMaxOccurs(1);

    final ParameterGroupDeclarer<?> params = licensingDef.onDefaultParameterGroup();
    params.withOptionalParameter(MULE_SDK_EXTENSION_REQUIRED_ENTITLEMENT_PARAMETER_NAME)
        .describedAs("The required entitlement in the customer extension license.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter(MULE_SDK_EXTENSION_REQUIRES_ENTERPRISE_LICENSE_PARAMETER_NAME)
        .describedAs("If the extension requires an enterprise license to run.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter(MULE_SDK_EXTENSION_ALLOWS_EVALUATION_LICENSE_PARAMETER_NAME)
        .describedAs("If the extension can be run with an evaluation license.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareXmlDslAttributesComponent(ComponentDeclarer<?, ?> def) {
    NestedComponentDeclarer<?, ?> xmlDslAttributesDef = def.withComponent(MULE_SDK_EXTENSION_XML_DSL_ATTRIBUTES_COMPONENT_NAME)
        .describedAs("Child element of the extension's description that allows customization of XML schema attributes.")
        .withMinOccurs(0)
        .withMaxOccurs(1);

    final ParameterGroupDeclarer<?> params = xmlDslAttributesDef.onDefaultParameterGroup();
    params.withOptionalParameter(MULE_SDK_EXTENSION_NAMESPACE_PARAMETER_NAME)
        .describedAs("Expected namespace of the extension to look for when generating the schemas. If left empty it will " +
            "default to http://www.mulesoft.org/schema/mule/[prefix], where [prefix] is the attribute prefix attribute value.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter(MULE_SDK_EXTENSION_PREFIX_PARAMETER_NAME)
        .describedAs("Expected prefix of the extension to look for when generating the schemas. If left empty it will create a " +
            "default one based on the extension's name, removing the words \"extension\", \"module\" or \"connector\" at " +
            "the end if they are present and hyphenizing the resulting name.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }
}
