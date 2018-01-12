/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.properties;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.processor.SimpleConfigAttribute;

import java.util.Map;

public interface ConfigurationPropertiesProviderFactory {

  ComponentIdentifier getComponentIdentifier();

  ConfigurationPropertiesProvider createProvider(Map<String, SimpleConfigAttribute> parameters,
                                                 ConfigurationPropertiesResolver localResolver,
                                                 ResourceProvider externalResourceProvider);
}
