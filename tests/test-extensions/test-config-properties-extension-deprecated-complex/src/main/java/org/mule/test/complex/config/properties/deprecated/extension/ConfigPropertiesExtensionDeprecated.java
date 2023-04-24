/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.complex.config.properties.deprecated.extension;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.sdk.api.annotation.dsl.xml.Xml;

@Extension(name = "Config Properties Deprecated")
@Xml(prefix = ConfigPropertiesExtensionDeprecated.NAMESPACE_PREFIX)
@Configurations(ConfigPropertiesExtensionDeprecatedConfig.class)
@Export(
    classes = {ComplexConfigurationPropertiesProviderFactory.class},
    resources = {"META-INF/services/org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory"})
public class ConfigPropertiesExtensionDeprecated {

  public static final String NAMESPACE_PREFIX = "config-properties-deprecated";

}
