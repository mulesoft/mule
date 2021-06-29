/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BASE_TYPE_BUILDER;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;

class OperationDslExtensionModelDeclarer {

  private static final String DSL_PREFIX = "operation-dsl";
  private static final String NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, DSL_PREFIX);
  private static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/operation-dsl/current/mule-operation-dsl.xsd";

  private static final String TYPE_EXAMPLE = "STRING or NUMBER or http:request-config";

  private static final MetadataType EXPRESSION_SUPPORT_TYPE = BASE_TYPE_BUILDER.stringType()
          .enumOf("SUPPORTED", "NOT_SUPPORTED", "REQUIRED")
          .build();

  private static final String OPERATION_STEREOTYPE_NAMESPACE = "OPERATION_DSL";
  private static final StereotypeModel OPERATION_STEREOTYPE = newStereotype("OPERATION", OPERATION_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel OPERATION_PARAMS_STEREOTYPE = newStereotype("PARAMETERS", OPERATION_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel OPERATION_PARAM_STEREOTYPE = newStereotype("PARAMETER", OPERATION_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel OUTPUT_STEREOTYPE = newStereotype("OUTPUT", OPERATION_STEREOTYPE_NAMESPACE).build();

  ExtensionDeclarer declareExtensionModel() {
    ExtensionDeclarer declarer = new ExtensionDeclarer()
            .named("Operations DSL")
            .describedAs("DSL for declaring Mule Operation")
            .onVersion(MULE_VERSION)
            .fromVendor(MULESOFT_VENDOR)
            .withCategory(COMMUNITY)
            .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
            .withXmlDsl(XmlDslModel.builder()
                    .setPrefix(DSL_PREFIX)
                    .setNamespace(NAMESPACE)
                    .setSchemaVersion(MULE_VERSION)
                    .setXsdFileName(DSL_PREFIX + ".xsd")
                    .setSchemaLocation(SCHEMA_LOCATION)
                    .build());

    declareOperationDef(declarer);

    return declarer;
  }

  private void declareOperationDef(ExtensionDeclarer declarer) {
    ConstructDeclarer construct = declarer.withConstruct("def")
            .describedAs("Defines an operation")
            .allowingTopLevelDefinition()
            .withStereotype(OPERATION_STEREOTYPE);

    ParameterGroupDeclarer parameters = construct.onDefaultParameterGroup();
    parameters.withRequiredParameter("name")
            .describedAs("The operation's name")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Operation name")
                    .summary("The operation's name")
                    .build())
            .withExpressionSupport(NOT_SUPPORTED)
            .ofType(STRING_TYPE)
            .asComponentId();

    parameters.withOptionalParameter("description")
            .ofType(STRING_TYPE)
            .describedAs("Detailed description of the operation purpose and behavior")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Description")
                    .summary("Detailed description of the operation purpose and behavior")
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED)
            .withLayout(LayoutModel.builder().asText().build());

    parameters.withOptionalParameter("summary")
            .ofType(STRING_TYPE)
            .describedAs("A brief description of the operation")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Summary")
                    .summary("A brief description of the operation")
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED);

    parameters.withOptionalParameter("public")
            .ofType(BOOLEAN_TYPE)
            .describedAs("Whether the operation is public and should be usable by third party components")
            .defaultingTo(false)
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Public")
                    .summary("Whether the operation is public and should be usable by third party components")
                    .build());

    declarer.withConstruct("parameters")
            .withStereotype(OPERATION_PARAMS_STEREOTYPE)
            .withComponent("parameter")
            .withAllowedStereotypes(OPERATION_PARAM_STEREOTYPE)
            .describedAs("Groups the operation's parameters");

    declareParameterConstruct(declarer);
  }

  private void declareParameterConstruct(ExtensionDeclarer declarer) {
    ParameterGroupDeclarer group = declarer.withConstruct("parameter")
            .withStereotype(OPERATION_PARAM_STEREOTYPE)
            .describedAs("Defines an operation parameter")
            .onDefaultParameterGroup();

    group.withRequiredParameter("name")
            .describedAs("The parameter's name")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Parameter name")
                    .summary("The parameter's name")
                    .build())
            .withExpressionSupport(NOT_SUPPORTED)
            .ofType(STRING_TYPE)
            .asComponentId();

    group.withOptionalParameter("description")
            .ofType(STRING_TYPE)
            .describedAs("Detailed description of the parameter, it's semantics, usage and effects")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Parameter description")
                    .summary("Detailed description of the parameter, it's semantics, usage and effects")
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED)
            .withLayout(LayoutModel.builder().asText().build());

    group.withOptionalParameter("summary")
            .ofType(STRING_TYPE)
            .describedAs("A brief description of the parameter")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Summary")
                    .summary("A brief description of the parameter")
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED);

    group.withRequiredParameter("type")
            .describedAs("The parameter's type")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Parameter type")
                    .summary("The Parameter's type")
                    .example(TYPE_EXAMPLE)
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED);

    group.withOptionalParameter("optional")
            .describedAs("Whether the parameter is optional or required.")
            .defaultingTo(false)
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Optional")
                    .summary("Whether the parameter is optional or required.")
                    .build())
            .ofType(BOOLEAN_TYPE)
            .withExpressionSupport(NOT_SUPPORTED);

    group.withOptionalParameter("expressionSupport")
            .describedAs("The support level this parameter offers regarding expressions")
            .ofType(EXPRESSION_SUPPORT_TYPE)
            .defaultingTo("NOT_SUPPORTED")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Expression Support")
                    .summary("The support level this parameter offers regarding expressions")
                    .build())
            .withExpressionSupport(NOT_SUPPORTED);

    group.withOptionalParameter("configOverride")
            .describedAs("Whether the parameter should act as a Config Override.")
            .ofType(BOOLEAN_TYPE)
            .defaultingTo(false)
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Config Override")
                    .summary("Whether the parameter should act as a Config Override.")
                    .build())
            .withExpressionSupport(NOT_SUPPORTED);
  }
}
