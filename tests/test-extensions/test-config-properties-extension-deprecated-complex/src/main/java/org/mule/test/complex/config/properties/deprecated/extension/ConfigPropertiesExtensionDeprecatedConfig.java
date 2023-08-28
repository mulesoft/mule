/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.complex.config.properties.deprecated.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.test.complex.config.properties.deprecated.extension.connection.ComplexTypesConnectionProvider;

@Configuration
@ConnectionProviders(ComplexTypesConnectionProvider.class)
public class ConfigPropertiesExtensionDeprecatedConfig {

}
