/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.core.api.MuleContext;

import java.util.Map;
import java.util.Optional;

public interface ConfigurationBuilderRegistryFilter {

  Optional<Object> filterRegisterObject(String serviceId, Object serviceImpl);

  Map<String, Object> getAdditionalObjectsToRegister(MuleContext muleContext);

}
