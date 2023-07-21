/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

@ConnectionProviders(FailingConnectionConnectionProvider.class)
@Sources(FailingConnectionSource.class)
@Configuration(name = "failing-connection-config")
public class FailingConnectionConfiguration {
}
