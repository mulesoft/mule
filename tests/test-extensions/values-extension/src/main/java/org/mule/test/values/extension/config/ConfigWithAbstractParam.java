/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.test.values.extension.AbstractParamOperations;
import org.mule.test.values.extension.connection.ConnectionProviderWithAbstractParam;

@Configuration(name = "abstract-param-config")
@ConnectionProviders(ConnectionProviderWithAbstractParam.class)
@Operations(AbstractParamOperations.class)
public class ConfigWithAbstractParam {

}
