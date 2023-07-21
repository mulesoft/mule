/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple operations since
 * they represent something core from the extension.
 */
@ConnectionProviders(LongDisconnectionConnectionProvider.class)
@Sources(SynchronizableSource.class)
@Configuration(name = "sync-config")
public class SynchronizableConfiguration {

}
