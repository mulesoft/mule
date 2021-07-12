/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.localisation.properties.extension;

import static org.mule.metadata.java.api.JavaTypeLoader.JAVA;
import static org.mule.runtime.api.meta.Category.COMMUNITY;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;

public class TestLocalisationPropertiesExtensionLoadingDelegate implements ExtensionLoadingDelegate {

  public static final String EXTENSION_NAME = "crafted-localisation-properties";

  @Override
  public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
    ConfigurationDeclarer configurationDeclarer = extensionDeclarer.named(EXTENSION_NAME)
        .describedAs("Crafted Localisation Properties Extension")
        .onVersion("1.0.0")
        .withCategory(COMMUNITY)
        .fromVendor("Mulesoft")
        .withConfig("localisation-configuration-properties");
    ParameterGroupDeclarer defaultParameterGroup = configurationDeclarer.onDefaultParameterGroup();
    defaultParameterGroup
        .withRequiredParameter("file").ofType(BaseTypeBuilder.create(JAVA).stringType().build());
    ParameterGroupDeclarer parameterGroupDeclarer =
        configurationDeclarer.onParameterGroup("language").withDslInlineRepresentation(true);
    parameterGroupDeclarer.withRequiredParameter("locale").ofType(BaseTypeBuilder.create(JAVA).stringType().build());
  }
}
