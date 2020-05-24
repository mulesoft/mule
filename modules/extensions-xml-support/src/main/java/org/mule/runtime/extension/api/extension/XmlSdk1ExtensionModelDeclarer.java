/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.extension;

import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.BODY;
import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.java.api.JavaTypeLoader;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;

/**
 * An {@link ExtensionDeclarer} for Mule's XML SDK v1
 *
 * @since 4.4
 */
public class XmlSdk1ExtensionModelDeclarer {

  public ExtensionDeclarer createExtensionModel() {
    final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault()
        .createTypeLoader(XmlSdk1ExtensionModelDeclarer.class
            .getClassLoader());
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

    final ConstructDeclarer operationDeclaration = extensionDeclarer.withConstruct("operation");
    operationDeclaration
        .allowingTopLevelDefinition()
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .asComponentId()
        .ofType(typeBuilder.stringType().build());

    operationDeclaration.withOptionalComponent("body")
        .withAllowedStereotypes(BODY);

    extensionDeclarer.withConstruct("body")
        .withStereotype(BODY)
        .withChain();

    return extensionDeclarer;
  }

}
