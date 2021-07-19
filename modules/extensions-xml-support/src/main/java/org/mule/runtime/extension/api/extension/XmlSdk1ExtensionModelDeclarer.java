/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.extension;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.BOOLEAN_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.INTEGER_TYPE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.STRING_TYPE;
import static org.mule.runtime.extension.api.extension.XmlSdkTypesValueProvider.ID;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;
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

/**
 * An {@link ExtensionDeclarer} for Mule's XML SDK v1
 *
 * @since 4.4
 */
public class XmlSdk1ExtensionModelDeclarer {

  private static final String XMLSDK1_STEREOTYPE_NAMESPACE = "XML_SDK_1";

  private static final StereotypeModel PARAMS_STEREOTYPE = newStereotype("PARAMETERS", XMLSDK1_STEREOTYPE_NAMESPACE).build();
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
            .setPrefix("module")
            .setNamespace("http://www.mulesoft.org/schema/mule/module")
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName("mule-module.xsd")
            .setSchemaLocation(buildSchemaLocation("module", "http://www.mulesoft.org/schema/mule/module"))
            .build());

    declarePropertyElement(extensionDeclarer, typeBuilder);
    declareOperation(typeBuilder, extensionDeclarer);

    return extensionDeclarer;
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
        // TODO: MULE-19605
        .withRoute("parameter")
        .withMinOccurs(0)
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
        // TODO: MULE-19605
        .withRoute("error")
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
        .allowingTopLevelDefinition();

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
