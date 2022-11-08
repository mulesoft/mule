/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.extension;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.INTEGER_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.extension.api.extension.XmlSdkTypesValueProvider.ID;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;
import static org.mule.runtime.extension.internal.dsl.xml.XmlDslConstants.MODULE_DSL_NAMESPACE;
import static org.mule.runtime.extension.internal.dsl.xml.XmlDslConstants.MODULE_DSL_NAMESPACE_URI;
import static org.mule.runtime.extension.internal.dsl.xml.XmlDslConstants.MODULE_ROOT_NODE_NAME;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.OperationVisibility.PRIVATE;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.OperationVisibility.PUBLIC;
import static org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty.builder;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.property.NoWrapperModelProperty;

/**
 * An {@link ExtensionDeclarer} for Mule's XML SDK v1
 *
 * @since 4.4
 */
public class XmlSdk1ExtensionModelDeclarer {

  private static final String XMLSDK1_STEREOTYPE_NAMESPACE = "XML_SDK_1";

  private static final StereotypeModel PARAMS_STEREOTYPE = newStereotype("PARAMETERS", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel PROPERTY_STEREOTYPE = newStereotype("PROPERTY", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel ERRORS_STEREOTYPE = newStereotype("ERRORS", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel OUTPUT_STEREOTYPE = newStereotype("OUTPUT", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel OUTPUT_ATTRIBUTES_STEREOTYPE =
      newStereotype("OUTPUT-ATTRIBUTES", XMLSDK1_STEREOTYPE_NAMESPACE).build();

  private final ValueProviderModel typesValueProvider = createTypesValueProviderModel();

  public ExtensionDeclarer createExtensionModel() {
    final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JavaTypeLoader.JAVA);

    ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer()
        .named("module")
        .describedAs("Mule Runtime and Integration Platform: XML SDK v1")
        .onVersion(MULE_VERSION)
        .fromVendor("MuleSoft, Inc.")
        .withCategory(SELECT)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(MODULE_DSL_NAMESPACE)
            .setNamespace(MODULE_DSL_NAMESPACE_URI)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName("mule-module.xsd")
            .setSchemaLocation(buildSchemaLocation(MODULE_DSL_NAMESPACE, MODULE_DSL_NAMESPACE_URI))
            .build());

    declareModuleConstruct(extensionDeclarer, typeBuilder);
    declarePropertyElement(extensionDeclarer, typeBuilder);
    declareOperation(typeBuilder, extensionDeclarer);
    declareConnectionConstruct(extensionDeclarer);

    return extensionDeclarer;
  }

  private void declareConnectionConstruct(ExtensionDeclarer extensionDeclarer) {
    extensionDeclarer.withConstruct("connection")
        .describedAs("A connection defines a set of properties that will be tight to the connection provider mechanism rather " +
            "than the configuration (default behaviour).")
        .allowingTopLevelDefinition()
        .withOptionalComponent("properties")
        .withAllowedStereotypes(PROPERTY_STEREOTYPE)
        .withMinOccurs(0)
        .withMaxOccurs(null)
        .withModelProperty(NoWrapperModelProperty.INSTANCE);
  }

  private void declareModuleConstruct(ExtensionDeclarer extensionDeclarer, BaseTypeBuilder typeBuilder) {
    ConstructDeclarer module = extensionDeclarer.withConstruct(MODULE_ROOT_NODE_NAME)
        .describedAs("A module is defined by three types of elements: properties, global elements and operations.")
        .allowingTopLevelDefinition();

    final ParameterGroupDeclarer params = module.onDefaultParameterGroup();
    params.withRequiredParameter("name")
        .describedAs("Name of the module that identifies it.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .asComponentId();

    params.withOptionalParameter("category")
        .describedAs("Set of defined categories for a module.")
        .ofType(typeBuilder.stringType().enumOf("COMMUNITY", "SELECT", "PREMIUM", "CERTIFIED").build())
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("COMMUNITY");

    params.withOptionalParameter("vendor")
        .describedAs("Expected vendor of the module.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED)
        .defaultingTo("MuleSoft");

    params.withOptionalParameter("requiredEntitlement")
        .describedAs("The required entitlement in the customer module license.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(false)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("allowsEvaluationLicense")
        .describedAs("If the module can be run with an evaluation license.")
        .ofType(BOOLEAN_TYPE)
        .defaultingTo(true)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("namespace")
        .describedAs("Expected namespace of the module to look for when generating the schemas. If left empty it will " +
            "default to http://www.mulesoft.org/schema/mule/[prefix], where [prefix] is the attribute prefix attribute value.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);

    params.withOptionalParameter("prefix")
        .describedAs("Expected prefix of the module to look for when generating the schemas. If left empty it will create a " +
            "default one based on the extension's name, removing the words \"extension\", \"module\" or \"connector\" at " +
            "the end if they are present and hyphenizing the resulting name.")
        .ofType(STRING_TYPE)
        .withExpressionSupport(NOT_SUPPORTED);
  }

  private void declareOperation(BaseTypeBuilder typeBuilder, ExtensionDeclarer extensionDeclarer) {
    final ConstructDeclarer operationDeclaration = extensionDeclarer.withConstruct("operation")
        .allowingTopLevelDefinition();

    final ParameterGroupDeclarer operationDefaultParamGroup = operationDeclaration.onDefaultParameterGroup();
    operationDefaultParamGroup
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(STRING_TYPE)
        .describedAs("Every operation must be named so that it can be called in a mule application.");

    addDisplayParams(operationDefaultParamGroup);
    operationDefaultParamGroup
        .withOptionalParameter("visibility")
        .defaultingTo(PUBLIC.name())
        .ofType(typeBuilder.stringType()
            .enumOf(PRIVATE.name(), PUBLIC.name())
            .build())
        .describedAs("Describes weather the operation can be accessible outside the module or not.");

    final ParameterGroupDeclarer parameterDefaultParamGroup = operationDeclaration.withOptionalComponent("parameters")
        .withStereotype(PARAMS_STEREOTYPE)
        .withOptionalComponent("parameter")
        .withMaxOccurs(null)
        .onDefaultParameterGroup();

    parameterDefaultParamGroup
        .withRequiredParameter("name")
        .ofType(STRING_TYPE);
    configureTypeParameter(parameterDefaultParamGroup.withRequiredParameter("type"));
    parameterDefaultParamGroup
        .withOptionalParameter("defaultValue")
        .ofType(STRING_TYPE);
    parameterDefaultParamGroup
        .withOptionalParameter("use")
        .defaultingTo("AUTO")
        .ofType(typeBuilder.stringType()
            .enumOf("REQUIRED", "OPTIONAL", "AUTO")
            .build());
    parameterDefaultParamGroup
        .withOptionalParameter("role")
        .defaultingTo("BEHAVIOUR")
        .ofType(typeBuilder.stringType()
            .enumOf("BEHAVIOUR", "CONTENT", "PRIMARY")
            .build());
    parameterDefaultParamGroup
        .withOptionalParameter("password")
        .defaultingTo(false)
        .ofType(BOOLEAN_TYPE);

    addDisplayParams(parameterDefaultParamGroup);
    parameterDefaultParamGroup
        .withOptionalParameter("order")
        .ofType(INTEGER_TYPE);
    parameterDefaultParamGroup
        .withOptionalParameter("tab")
        .defaultingTo(Placement.DEFAULT_TAB)
        .ofType(STRING_TYPE);

    configureTypeParameter(operationDeclaration.withOptionalComponent("output")
        .describedAs("Defines the output of the operation if exists, void otherwise.")
        .withStereotype(OUTPUT_STEREOTYPE)
        .onDefaultParameterGroup()
        .withRequiredParameter("type"));

    configureTypeParameter(operationDeclaration.withOptionalComponent("output-attributes")
        .describedAs("Defines the attribute's output of the operation if exists, void otherwise.")
        .withStereotype(OUTPUT_ATTRIBUTES_STEREOTYPE)
        .onDefaultParameterGroup()
        .withRequiredParameter("type"));

    operationDeclaration.withChain("body");

    operationDeclaration.withOptionalComponent("errors")
        .withStereotype(ERRORS_STEREOTYPE)
        .describedAs("Collection of errors that might be thrown by the current operation.")
        .withOptionalComponent("error")
        .withMaxOccurs(null)
        .onDefaultParameterGroup()
        .withRequiredParameter("type")
        .ofType(STRING_TYPE)
        .describedAs("Defined error for the current operation.");
  }

  private ParameterDeclarer configureTypeParameter(ParameterDeclarer parameter) {
    return parameter.ofType(STRING_TYPE).withValueProviderModel(typesValueProvider);
  }

  private ValueProviderModel createTypesValueProviderModel() {
    return new ValueProviderModel(emptyList(),
                                  false,
                                  false,
                                  true, 0,
                                  ID,
                                  ID,
                                  builder(XmlSdkTypesValueProvider.class).build());
  }

  private void declarePropertyElement(ExtensionDeclarer extensionDeclarer, BaseTypeBuilder typeBuilder) {
    final ConstructDeclarer propertyDeclaration = extensionDeclarer.withConstruct("property")
        .describedAs("A property element defines an input value for the operation in which it is define. Such property must be " +
            "defined with a meaningful name, a type which defines the kind of content the property must have and optionally a " +
            "default value that will be used if the invocation to the operation does not defines a value for the property. " +
            "The property can be accessed within the body definition of the operation using an expression such as " +
            "#[property.paramName]")
        .allowingTopLevelDefinition()
        .withStereotype(PROPERTY_STEREOTYPE);

    final ParameterGroupDeclarer propertyDeclarationDefaultParamGroup = propertyDeclaration.onDefaultParameterGroup();

    propertyDeclarationDefaultParamGroup
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(STRING_TYPE);
    configureTypeParameter(propertyDeclarationDefaultParamGroup
        .withRequiredParameter("type"));
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("defaultValue")
        .ofType(STRING_TYPE);
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("use")
        .defaultingTo("AUTO")
        .ofType(typeBuilder.stringType()
            .enumOf("REQUIRED", "OPTIONAL", "AUTO")
            .build());

    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("password")
        .defaultingTo(false)
        .ofType(BOOLEAN_TYPE);

    addDisplayParams(propertyDeclarationDefaultParamGroup);
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("order")
        .ofType(INTEGER_TYPE);
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("tab")
        .defaultingTo(Placement.DEFAULT_TAB)
        .ofType(STRING_TYPE);
  }

  private void addDisplayParams(final ParameterGroupDeclarer ownerParamGroup) {
    ownerParamGroup
        .withOptionalParameter("displayName")
        .ofType(STRING_TYPE)
        .describedAs("Display name of the operation. It can be any string. When empty, it will default to an auto generated one from the name attribute.");
    ownerParamGroup
        .withOptionalParameter("summary")
        .ofType(STRING_TYPE)
        .describedAs("A very brief overview about this operation.");
    ownerParamGroup
        .withOptionalParameter("example")
        .ofType(STRING_TYPE)
        .describedAs("An example about the content of this operation.");
  }
}
