/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.extension;

import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.PRIMITIVE_TYPES;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CHAIN;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.OperationVisibility.PRIVATE;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.OperationVisibility.PUBLIC;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.property.SkipOnWalkModelProperty;

/**
 * An {@link ExtensionDeclarer} for Mule's XML SDK v1
 *
 * @since 4.4
 */
public class XmlSdk1ExtensionModelDeclarer {

  private static final String XMLSDK1_STEREOTYPE_NAMESPACE = "XML_SDK_1";

  private static final StereotypeModel PARAMS_STEREOTYPE = newStereotype("PARAMETERS", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel PARAM_STEREOTYPE = newStereotype("PARAMETER", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel ERRORS_STEREOTYPE = newStereotype("ERRORS", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel ERROR_STEREOTYPE = newStereotype("ERROR", XMLSDK1_STEREOTYPE_NAMESPACE).build();
  private static final StereotypeModel OUTPUT_STEREOTYPE = newStereotype("OUTPUT", XMLSDK1_STEREOTYPE_NAMESPACE).build();

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

    final ConstructDeclarer propertyDeclaration = extensionDeclarer.withConstruct("property");
    final ParameterGroupDeclarer propertyDeclarationDefaultParamGroup = propertyDeclaration
        .allowingTopLevelDefinition()
        .onDefaultParameterGroup();
    propertyDeclarationDefaultParamGroup
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(typeBuilder.stringType().build());
    propertyDeclarationDefaultParamGroup
        .withRequiredParameter("type")
        .ofType(typeBuilder.stringType()
            .enumOf(PRIMITIVE_TYPES.keySet().toArray(new String[PRIMITIVE_TYPES.size()]))
            .build());
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("defaultValue")
        .ofType(typeBuilder.stringType().build());
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("use")
        .defaultingTo("AUTO")
        .ofType(typeBuilder.stringType()
            .enumOf("REQUIRED", "OPTIONAL", "AUTO")
            .build());

    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("password")
        .defaultingTo(false)
        .ofType(typeBuilder.booleanType().build());

    addDisplayParams(typeBuilder, propertyDeclarationDefaultParamGroup);
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("order")
        .ofType(typeBuilder.numberType().build());
    propertyDeclarationDefaultParamGroup
        .withOptionalParameter("tab")
        .defaultingTo(Placement.DEFAULT_TAB)
        .ofType(typeBuilder.stringType().build());

    final ConstructDeclarer operationDeclaration = extensionDeclarer.withConstruct("operation");
    final ParameterGroupDeclarer operationDefaultParamGroup = operationDeclaration
        .allowingTopLevelDefinition()
        .onDefaultParameterGroup();
    operationDefaultParamGroup
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(typeBuilder.stringType().build())
        .describedAs("Every operation must be named so that it can be called in a mule application.");

    addDisplayParams(typeBuilder, operationDefaultParamGroup);
    operationDefaultParamGroup
        .withOptionalParameter("visibility")
        .defaultingTo(PUBLIC.name())
        .ofType(typeBuilder.stringType()
            .enumOf(PRIVATE.name(), PUBLIC.name())
            .build())
        .describedAs("Describes weather the operation can be accessible outside the module or not.");

    operationDeclaration.withOptionalComponent("parameters")
        .withAllowedStereotypes(PARAMS_STEREOTYPE);

    extensionDeclarer.withConstruct("parameters")
        .withStereotype(PARAMS_STEREOTYPE)
        .withComponent("parameter")
        .withAllowedStereotypes(PARAM_STEREOTYPE);

    final ParameterGroupDeclarer parameterDefaultParamGroup = extensionDeclarer.withConstruct("parameter")
        .withStereotype(PARAM_STEREOTYPE)
        .onDefaultParameterGroup();

    parameterDefaultParamGroup
        .withRequiredParameter("name")
        .ofType(typeBuilder.stringType().build());
    parameterDefaultParamGroup
        .withRequiredParameter("type")
        .ofType(typeBuilder.stringType()
            .enumOf(PRIMITIVE_TYPES.keySet().toArray(new String[PRIMITIVE_TYPES.size()]))
            .build());
    parameterDefaultParamGroup
        .withOptionalParameter("defaultValue")
        .ofType(typeBuilder.stringType().build());
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
        .ofType(typeBuilder.booleanType().build());

    addDisplayParams(typeBuilder, parameterDefaultParamGroup);
    parameterDefaultParamGroup
        .withOptionalParameter("order")
        .ofType(typeBuilder.numberType().build());
    parameterDefaultParamGroup
        .withOptionalParameter("tab")
        .defaultingTo(Placement.DEFAULT_TAB)
        .ofType(typeBuilder.stringType().build());

    operationDeclaration.withOptionalComponent("body")
        .withAllowedStereotypes(CHAIN);

    extensionDeclarer.withConstruct("body")
        .withStereotype(CHAIN)
        .withChain();

    operationDeclaration.withOptionalComponent("output")
        .withModelProperty(new SkipOnWalkModelProperty())
        .withAllowedStereotypes(OUTPUT_STEREOTYPE);

    extensionDeclarer.withConstruct("output")
        .withStereotype(OUTPUT_STEREOTYPE)
        .onDefaultParameterGroup()
        .withRequiredParameter("type")
        .ofType(typeBuilder.stringType()
            .enumOf(PRIMITIVE_TYPES.keySet().toArray(new String[PRIMITIVE_TYPES.size()]))
            .build());

    operationDeclaration.withOptionalComponent("errors")
        .withAllowedStereotypes(ERRORS_STEREOTYPE)
        .describedAs("Collection of errors that might be thrown by the current operation.");

    extensionDeclarer.withConstruct("errors")
        .withStereotype(ERRORS_STEREOTYPE)
        .withComponent("error")
        .withAllowedStereotypes(ERROR_STEREOTYPE);

    final ParameterGroupDeclarer errorDefaultParamGroup = extensionDeclarer.withConstruct("error")
        .withStereotype(ERROR_STEREOTYPE)
        .onDefaultParameterGroup();

    errorDefaultParamGroup
        .withRequiredParameter("type")
        .ofType(typeBuilder.stringType().build())
        .describedAs("Defined error for the current operation.");

    return extensionDeclarer;
  }

  private void addDisplayParams(final BaseTypeBuilder typeBuilder, final ParameterGroupDeclarer ownerParamGroup) {
    ownerParamGroup
        .withOptionalParameter("displayName")
        .ofType(typeBuilder.stringType().build())
        .describedAs("Display name of the operation. It can be any string. When empty, it will default to an auto generated one from the name attribute.");
    ownerParamGroup
        .withOptionalParameter("summary")
        .ofType(typeBuilder.stringType().build())
        .describedAs("A very brief overview about this operation.");
    ownerParamGroup
        .withOptionalParameter("example")
        .ofType(typeBuilder.stringType().build())
        .describedAs("An example about the content of this operation.");
  }

}
