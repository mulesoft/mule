/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BASE_TYPE_BUILDER;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OPERATION_DEF_STEREOTYPE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OUTPUT_ATTRIBUTES_STEREOTYPE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OUTPUT_PAYLOAD_STEREOTYPE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.OUTPUT_STEREOTYPE;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import org.mule.metadata.api.annotation.DefaultValueAnnotation;
import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.api.declaration.type.annotation.DisplayTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.ExpressionSupportAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.LayoutTypeAnnotation;
import org.mule.runtime.extension.api.declaration.type.annotation.TypeDslAnnotation;

class AppOperationExtensionModelDeclarer {

  private static final String DSL_PREFIX = "operation";
  private static final String NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, DSL_PREFIX);
  private static final String SCHEMA_LOCATION =
          "http://www.mulesoft.org/schema/mule/operation-dsl/current/mule-operation-dsl.xsd";

  private static final String TYPE_EXAMPLE = "STRING or NUMBER or http:request-config";

  private static final MetadataType EXPRESSION_SUPPORT_TYPE = BASE_TYPE_BUILDER.stringType()
          .enumOf("SUPPORTED", "NOT_SUPPORTED", "REQUIRED")
          .build();

  ExtensionDeclarer declareExtensionModel() {
    ExtensionDeclarer declarer = new ExtensionDeclarer()
            .named("Application Operations DSL")
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
            .withStereotype(OPERATION_DEF_STEREOTYPE);

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

    parameters.withOptionalParameter("public")
            .ofType(BOOLEAN_TYPE)
            .describedAs("Whether the operation is public and should be usable by third party components")
            .defaultingTo(false)
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Public")
                    .summary("Whether the operation is public and should be usable by third party components")
                    .build());

    parameters.withOptionalParameter("summary")
            .ofType(STRING_TYPE)
            .describedAs("A brief description of the operation")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Summary")
                    .summary("A brief description of the operation")
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED);

    parameters.withOptionalParameter("displayName")
            .ofType(STRING_TYPE)
            .describedAs("The operation's name in the GUI")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Display Name")
                    .summary("The operation's name in the GUI")
                    .build());

    addParametersDeclaration(construct);
    declareOutputConstruct(declarer);

    construct.withChain("body")
            .describedAs("The operations that makes for the operation's implementation")
            .setRequired(true);
  }

  private void declareOutputConstruct(ExtensionDeclarer declarer) {
    // TODO: payload-type and attribute-types should be a nested constructs into the parent "output" one but ExtensionModel
    // doesn't have that capability yet. Same affects the XML SDK and Batch Extension Models. Everybody lies.
    ComponentDeclarer outputConstruct = declarer.withConstruct("output")
            .describedAs("Defines a operation's output types.")
            .withStereotype(OUTPUT_STEREOTYPE);

    outputConstruct.withComponent("payload-type-component")
            .withAllowedStereotypes(OUTPUT_PAYLOAD_STEREOTYPE)
            .describedAs("Type definition for the operation's output payload");

    declareOutputTypeParameters(declarer.withConstruct("payload-type")
            .describedAs("Type definition for the operation's output payload")
            .withStereotype(OUTPUT_PAYLOAD_STEREOTYPE)
            .onDefaultParameterGroup(), "payload");

    outputConstruct.withOptionalComponent("attributes-type-component")
            .withAllowedStereotypes(OUTPUT_ATTRIBUTES_STEREOTYPE)
            .describedAs("Type definition for the operation's output attributes");

    declareOutputTypeParameters(declarer.withConstruct("attributes-type")
            .describedAs("Type definition for the operation's output attributes")
            .withStereotype(OUTPUT_ATTRIBUTES_STEREOTYPE)
            .onDefaultParameterGroup(), "attributes");
  }

  private ParameterGroupDeclarer declareOutputTypeParameters(ParameterGroupDeclarer group, String type) {
    group.withRequiredParameter("type")
            .describedAs("The output " + type + " type")
            .withDisplayModel(DisplayModel.builder()
                    .displayName(capitalize(type) + " type")
                    .summary("The output " + type + " type")
                    .example(TYPE_EXAMPLE)
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED);

    group.withOptionalParameter("mimeType")
            .describedAs("The value media type")
            .withDisplayModel(DisplayModel.builder()
                    .displayName("Media Type")
                    .summary("The value media type")
                    .example("application/json")
                    .build())
            .ofType(STRING_TYPE)
            .withExpressionSupport(NOT_SUPPORTED);

    return group;
  }

  private void addParametersDeclaration(ParameterizedDeclarer declarer) {
    declarer.onDefaultParameterGroup().withOptionalParameter("parameters")
            .describedAs("The operation's parameters")
            .withExpressionSupport(NOT_SUPPORTED)
            .ofType(getOperationParametersArrayType());
  }

  private ArrayType getOperationParametersArrayType() {
    return BASE_TYPE_BUILDER.arrayType()
            .of(getOperationParameterMetadataType())
            .description("Groups the operation's parameters")
            .build();
  }

  private MetadataType getOperationParameterMetadataType() {
    ObjectTypeBuilder param = BASE_TYPE_BUILDER.objectType()
            .description("Defines an operation parameter")
            .id("<operation:parameter>")
            .with(new TypeDslAnnotation(true, false, "", ""))
            .with(new TypeAliasAnnotation("parameter"));

    param.addField()
            .key("name")
            .description("The parameter's name")
            .required()
            .value(STRING_TYPE)
            .with(displayAnnotation("Parameter name", "The parameter's name"))
            .with(expressionSupport(NOT_SUPPORTED));

    param.addField()
            .key("description")
            .description("Detailed description of the parameter, it's semantics, usage and effects")
            .required(false)
            .value(STRING_TYPE)
            .with(displayAnnotation("Parameter description", "Detailed description of the parameter, it's semantics, usage and effects"))
            .with(expressionSupport(NOT_SUPPORTED))
            .with(new LayoutTypeAnnotation(LayoutModel.builder().asText().build()));

    param.addField()
            .key("summary")
            .description("A brief description of the parameter")
            .required(false)
            .value(STRING_TYPE)
            .with(displayAnnotation("Summary", "A brief description of the parameter"))
            .with(expressionSupport(NOT_SUPPORTED));

    param.addField()
            .key("type")
            .description("The parameter's type")
            .required()
            .value(STRING_TYPE)
            .with(displayAnnotation("Parameter type", "The Parameter's type", TYPE_EXAMPLE))
            .with(expressionSupport(NOT_SUPPORTED));

    param.addField()
            .key("optional")
            .description("Indicates that the parameter is optional")
            .required(false)
            .value(getOptionalDeclarationMetadataType())
            .with(displayAnnotation("Optional", "Indicates that the parameter is optional"))
            .with(expressionSupport(NOT_SUPPORTED))
            .with(new TypeDslAnnotation(true, false, "", ""));

    param.addField()
            .key("expressionSupport")
            .description("The support level this parameter offers regarding expressions")
            .value(EXPRESSION_SUPPORT_TYPE)
            .required(false)
            .with(defaultValueAnnotation("NOT_SUPPORTED"))
            .with(displayAnnotation("Expression Support", "The support level this parameter offers regarding expressions"))
            .with(expressionSupport(NOT_SUPPORTED));

    param.addField()
            .key("configOverride")
            .description("Whether the parameter should act as a Config Override.")
            .value(BOOLEAN_TYPE)
            .required(false)
            .with(defaultValueAnnotation("false"))
            .with(displayAnnotation("Config Override", "Whether the parameter should act as a Config Override."))
            .with(expressionSupport(NOT_SUPPORTED));

    return param.build();
  }

  private MetadataType getOptionalDeclarationMetadataType() {
    ObjectTypeBuilder builder = BASE_TYPE_BUILDER.objectType()
            .id("<operation:optional>")
            .description("Indicates that the parameter is optional")
            .with(new TypeDslAnnotation(true, false, "", ""));

    builder.addField()
            .key("defaultValue")
            .description("The parameters default value is not provided.")
            .value(STRING_TYPE)
            .required(false)
            .with(expressionSupport(NOT_SUPPORTED))
            .with(displayAnnotation("Optional", "The parameters default value is not provided."));

    ObjectTypeBuilder exclusiveOptionals = BASE_TYPE_BUILDER.objectType()
            .id("<operation:exclusive-optional>")
            .description("References other optional parameters which cannot be set at the same time as this one")
            .with(new TypeDslAnnotation(true, false, "", ""))
            .with(new TypeAliasAnnotation("exclusive-optional"));

    exclusiveOptionals.addField()
            .key("parameters")
            .description("Comma separated list of parameters that this element is optional against")
            .value(STRING_TYPE)
            .required()
            .with(expressionSupport(NOT_SUPPORTED))
            .with(displayAnnotation("Parameters", "Comma separated list of parameters that this element is optional against"));

    exclusiveOptionals.addField()
            .key("oneRequired")
            .description("Enforces that one of the parameters must be set at any given time")
            .value(BOOLEAN_TYPE)
            .required(false)
            .with(expressionSupport(NOT_SUPPORTED))
            .with(displayAnnotation("One required?", "Enforces that one of the parameters must be set at any given time"));

    builder.addField()
            .key("exclusiveOptional")
            .value(exclusiveOptionals.build())
            .required(false)
            .with(expressionSupport(NOT_SUPPORTED))
            .with(displayAnnotation("Exclusive Optionals", "References other optional parameters which cannot be set at the same time as this one"));

    return builder.build();
  }

  private TypeAnnotation displayAnnotation(String displayName, String summary) {
    return displayAnnotation(displayName, summary, null);
  }

  private TypeAnnotation displayAnnotation(String displayName, String summary, String example) {
    return new DisplayTypeAnnotation(DisplayModel.builder()
            .displayName(displayName)
            .summary(summary)
            .example(example)
            .build());
  }

  private TypeAnnotation expressionSupport(ExpressionSupport expressionSupport) {
    return new ExpressionSupportAnnotation(expressionSupport);
  }

  private TypeAnnotation defaultValueAnnotation(String defaultValue) {
    return new DefaultValueAnnotation(defaultValue);
  }
}
