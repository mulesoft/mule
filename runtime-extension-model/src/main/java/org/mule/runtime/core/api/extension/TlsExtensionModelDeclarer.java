/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.extension;

import static org.mule.runtime.api.meta.Category.COMMUNITY;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULESOFT_VENDOR;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_TLS_NAMESPACE;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_TLS_SCHEMA_LOCATION;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.MULE_VERSION;
import static org.mule.runtime.internal.dsl.DslConstants.TLS_PREFIX;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.core.internal.extension.CustomBuildingDefinitionProviderModelProperty;

/**
 * An {@link ExtensionDeclarer} containing the namespace declaration for the tls module.
 *
 * @since 4.4
 */
class TlsExtensionModelDeclarer {

  ExtensionDeclarer createExtensionModel() {
    return new ExtensionDeclarer()
        .named(TLS_PREFIX)
        .describedAs("Mule Runtime and Integration Platform: TLS components")
        .onVersion(MULE_VERSION)
        .fromVendor(MULESOFT_VENDOR)
        .withCategory(COMMUNITY)
        .withModelProperty(new CustomBuildingDefinitionProviderModelProperty())
        .withXmlDsl(XmlDslModel.builder()
            .setPrefix(TLS_PREFIX)
            .setNamespace(MULE_TLS_NAMESPACE)
            .setSchemaVersion(MULE_VERSION)
            .setXsdFileName(TLS_PREFIX + ".xsd")
            .setSchemaLocation(MULE_TLS_SCHEMA_LOCATION)
            .build());
  }

}
