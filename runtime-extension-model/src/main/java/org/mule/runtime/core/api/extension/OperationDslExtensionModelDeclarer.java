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
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.TYPE_LOADER;
import static org.mule.runtime.internal.dsl.DslConstants.DEFAULT_NAMESPACE_URI_MASK;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;

class OperationDslExtensionModelDeclarer {

  public static final String DSL_PREFIX = "operation-dsl";
  private static final String NAMESPACE = format(DEFAULT_NAMESPACE_URI_MASK, DSL_PREFIX);
  private static final String SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/operation-dsl/current/mule-operation-dsl.xsd";

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

  private ConstructDeclarer declareOperationDef(ExtensionDeclarer declarer) {
    ConstructDeclarer construct = declarer.withConstruct("def")
            .describedAs("Defines an operation")
            .allowingTopLevelDefinition();

    construct.onDefaultParameterGroup()
            .withRequiredParameter("name")
            .describedAs("The operation's name")
            .withExpressionSupport(NOT_SUPPORTED)
            .ofType(TYPE_LOADER.load(String.class));

    construct.onDefaultParameterGroup().withOptionalParameter("description")
            .ofType()
  }
}
