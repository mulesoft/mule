/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_NAMESPACE;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_NAMESPACE_URI;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_SCHEMA_LOCATION;
import static org.mule.runtime.config.internal.dsl.processor.xml.OperationDslNamespaceInfoProvider.OPERATION_DSL_XSD_FILE_NAME;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BASE_TYPE_BUILDER;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.VOID_TYPE;
import static org.mule.runtime.extension.api.error.ErrorConstants.ERROR_TYPE_DEFINITION;
import static org.mule.sdk.api.meta.ExpressionSupport.SUPPORTED;
import static org.mule.sdk.api.stereotype.MuleStereotypes.DEPRECATED_STEREOTYPE;
import static org.mule.sdk.api.stereotype.MuleStereotypes.OPERATION_DEF_STEREOTYPE;
import static org.mule.sdk.api.stereotype.MuleStereotypes.OUTPUT_ATTRIBUTES_STEREOTYPE;
import static org.mule.sdk.api.stereotype.MuleStereotypes.OUTPUT_PAYLOAD_STEREOTYPE;
import static org.mule.sdk.api.stereotype.MuleStereotypes.OUTPUT_STEREOTYPE;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.internal.property.NoErrorMappingModelProperty;

/**
 * Builds the {@link ExtensionDeclarer} for the {@code operation} namespace used to define Mule DSL operations
 *
 * @since 4.5.0
 */
class MuleOperationExtensionModelDeclarer {

  private static final String TYPE_EXAMPLE = "string or number or http:request-config";

  private static final MetadataType EXPRESSION_SUPPORT_TYPE = BASE_TYPE_BUILDER.stringType()
      .enumOf("SUPPORTED", "NOT_SUPPORTED", "REQUIRED")
      .build();

  ExtensionDeclarer declareExtensionModel() {
    ExtensionDeclarer declarer = new ExtensionDeclarer()
        .named("Mule Operations DSL")
        .describedAs("DSL for declaring Mule Operation")
        .onVersion(MULE_VERSION)
        .fromVendor(MULESOFT_VENDOR)
        .withCategory(COMMUNITY)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(OPERATION_DSL_NAMESPACE)
            .setNamespace(OPERATION_DSL_NAMESPACE_URI)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName(OPERATION_DSL_XSD_FILE_NAME)
            .setSchemaLocation(OPERATION_DSL_SCHEMA_LOCATION)
            .build());

    declareOperationDef(declarer);

    return declarer;
  }

  private void declareOperationDef(ExtensionDeclarer declarer) {
    ConstructDeclarer def = declarer.withConstruct("def")
        .describedAs("Defines an operation")
        .allowingTopLevelDefinition()
        .withStereotype(OPERATION_DEF_STEREOTYPE);

    ParameterGroupDeclarer parameters = def.onDefaultParameterGroup();
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

    parameters.withOptionalParameter("displayName")
        .ofType(STRING_TYPE)
        .describedAs("The operation's name in the GUI")
        .withDisplayModel(DisplayModel.builder()
            .displayName("Display Name")
            .summary("The operation's name in the GUI")
            .build());

    parameters.withOptionalParameter("visibility")
        .ofType(BASE_TYPE_BUILDER.stringType()
            .enumOf("PUBLIC", "PRIVATE")
            .build())
        .describedAs("The operation visibility to third parties")
        .withDisplayModel(DisplayModel.builder()
            .displayName("Visibility")
            .summary("The operation visibility to third parties")
            .build());


    addParametersDeclaration(def);
    declareOutputConstruct(def);
    declareDeprecationConstruct(def, "Defines an operation's deprecation.");

    def.withChain("body")
        .describedAs("The operations that makes for the operation's implementation")
        .setRequired(true);

    declareRaiseError(declarer);
  }

  private void declareOutputConstruct(ConstructDeclarer def) {
    NestedComponentDeclarer outputConstruct = def.withComponent("output")
        .describedAs("Defines a operation's output types.")
        .withStereotype(OUTPUT_STEREOTYPE)
        .withMinOccurs(1)
        .withMaxOccurs(1);

    NestedComponentDeclarer payloadType = outputConstruct.withComponent("payload-type")
        .describedAs("Type definition for the operation's output payload")
        .withStereotype(OUTPUT_PAYLOAD_STEREOTYPE)
        .withMinOccurs(1)
        .withMaxOccurs(1);

    declareOutputTypeParameters(payloadType, "payload");

    NestedComponentDeclarer attributesType = outputConstruct.withOptionalComponent("attributes-type")
        .describedAs("Type definition for the operation's output attributes")
        .withStereotype(OUTPUT_ATTRIBUTES_STEREOTYPE)
        .withMinOccurs(0)
        .withMaxOccurs(1);

    declareOutputTypeParameters(attributesType, "attributes");
  }

  private void declareDeprecationConstruct(ComponentDeclarer def, String description) {
    NestedComponentDeclarer deprecationConstruct = def.withComponent("deprecated")
        .describedAs(description)
        .withStereotype(DEPRECATED_STEREOTYPE)
        .withMinOccurs(0)
        .withMaxOccurs(1);

    ParameterGroupDeclarer defaultParameterGroupDeclarer = deprecationConstruct.onDefaultParameterGroup();
    defaultParameterGroupDeclarer.withRequiredParameter("message")
        .describedAs("Describes why something was deprecated, what can be used as substitute, or both")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    defaultParameterGroupDeclarer.withRequiredParameter("since")
        .describedAs("The version of the extension in which the annotated member was deprecated")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    defaultParameterGroupDeclarer.withOptionalParameter("toRemoveIn")
        .describedAs("The version of the extension in which the annotated member will be removed or was removed")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareRaiseError(ExtensionDeclarer extensionDeclarer) {
    OperationDeclarer raiseError = extensionDeclarer.withOperation("raiseError")
            .describedAs("Throws an error with the specified type and description.")
            .withModelProperty(new NoErrorMappingModelProperty());

    raiseError.withOutput().ofType(VOID_TYPE);
    raiseError.withOutputAttributes().ofType(VOID_TYPE);

    raiseError.onDefaultParameterGroup()
            .withRequiredParameter("type")
            .ofType(ERROR_TYPE_DEFINITION)
            .withExpressionSupport(NOT_SUPPORTED)
            .describedAs("The error type to raise.");

    raiseError.onDefaultParameterGroup()
            .withOptionalParameter("description")
            .ofType(STRING_TYPE)
            .describedAs("The description of this error.");
  }

  private void declareOutputTypeParameters(NestedComponentDeclarer component, String outputRole) {
    component.onDefaultParameterGroup().withRequiredParameter("type")
        .describedAs("The output " + outputRole + " type")
        .withDisplayModel(DisplayModel.builder()
            .displayName(capitalize(outputRole) + " type")
            .summary("The output " + outputRole + " type")
            .example(TYPE_EXAMPLE)
            .build())
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private DisplayModel display(String displayName, String summary) {
    return DisplayModel.builder()
        .displayName(displayName)
        .summary(summary)
        .build();
  }

  private DisplayModel display(String displayName, String summary, String example) {
    return DisplayModel.builder()
        .displayName(displayName)
        .summary(summary)
        .example(example)
        .build();
  }

  private void addParametersDeclaration(ConstructDeclarer def) {
    final NestedComponentDeclarer parametersDef = def.withOptionalComponent("parameters")
        .describedAs("The operation's parameters")
        .withMinOccurs(0)
        .withMaxOccurs(1);

    final NestedComponentDeclarer parameterDef = parametersDef.withComponent("parameter")
        .describedAs("Defines an operation parameter")
        .withMinOccurs(1)
        .withMaxOccurs(null);

    declareDeprecationConstruct(parameterDef, "Defines a parameter's deprecation.");

    final ParameterGroupDeclarer parameterDefParameters = parameterDef.onDefaultParameterGroup();
    addParameterDeclaration(parameterDefParameters);

    final NestedComponentDeclarer optionalParameterDef = parametersDef.withOptionalComponent("optional-parameter")
        .describedAs("Defines an optional operation parameter")
        .withMinOccurs(0)
        .withMaxOccurs(null);

    final ParameterGroupDeclarer optionalParameterDefParameters = optionalParameterDef.onDefaultParameterGroup();
    addOptionalParameterDeclaration(optionalParameterDefParameters);

    final NestedComponentDeclarer exclusiveOptionalDef = parametersDef.withOptionalComponent("exclusive-optionals")
        .describedAs("Defines a set of mutually exclusive parameters")
        .withMinOccurs(0)
        .withMaxOccurs(1);

    exclusiveOptionalDef.onDefaultParameterGroup().withRequiredParameter("exclusiveOptionals")
        .describedAs("Comma separated list of parameters that are mutually exclusive")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(display("Parameters", "Comma separated list of parameters that this element is optional against"));

    exclusiveOptionalDef.onDefaultParameterGroup().withOptionalParameter("oneRequired")
        .describedAs("Enforces that one of the parameters must be set at any given time")
        .ofType(BOOLEAN_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(display("One required?", "Enforces that one of the parameters must be set at any given time"));
  }

  private void addParameterDeclaration(ParameterGroupDeclarer<?> parameterGroupDeclarer) {
    parameterGroupDeclarer.withRequiredParameter("name")
        .describedAs("The parameter's name")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(display("Parameter name", "The parameter's name"));

    parameterGroupDeclarer.withOptionalParameter("description")
        .describedAs("Detailed description of the parameter, it's semantics, usage and effects")
        .ofType(STRING_TYPE)
        .withDisplayModel(display("Parameter description",
                                  "Detailed description of the parameter, it's semantics, usage and effects"))
        .withExpressionSupport(NOT_SUPPORTED)
        .withLayout(LayoutModel.builder().asText().build());

    parameterGroupDeclarer.withOptionalParameter("summary")
        .describedAs("A brief description of the parameter")
        .ofType(STRING_TYPE)
        .withDisplayModel(display("Summary", "A brief description of the parameter"))
        .withExpressionSupport(NOT_SUPPORTED);

    parameterGroupDeclarer.withOptionalParameter("type")
        .describedAs("The parameter's type")
        .ofType(STRING_TYPE)
        .withDisplayModel(display("Parameter type", "The Parameter's type", TYPE_EXAMPLE))
        .withExpressionSupport(NOT_SUPPORTED);

    parameterGroupDeclarer.withOptionalParameter("expressionSupport")
        .describedAs("The support level this parameter offers regarding expressions")
        .ofType(EXPRESSION_SUPPORT_TYPE)
        .defaultingTo(SUPPORTED.name())
        .withDisplayModel(display("Expression Support", "The support level this parameter offers regarding expressions"))
        .withExpressionSupport(NOT_SUPPORTED);

    parameterGroupDeclarer.withOptionalParameter("configOverride")
        .describedAs("Whether the parameter should act as a Config Override.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo("false")
        .withDisplayModel(display("Config Override", "Whether the parameter should act as a Config Override."))
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void addOptionalParameterDeclaration(ParameterGroupDeclarer<?> parameterGroupDeclarer) {
    addParameterDeclaration(parameterGroupDeclarer);

    parameterGroupDeclarer.withOptionalParameter("defaultValue")
        .describedAs("The parameter's default value if not provided.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .withDisplayModel(display("Optional", "The parameter's default value if not provided."));
  }
}
