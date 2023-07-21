/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;


/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations and
 * sources are going to be declared.
 */
@Extension(name = "reconnection")
@Configurations({ReconnectionConfiguration.class, SynchronizableConfiguration.class, FailingConnectionConfiguration.class})
public class ReconnectionExtension {

}
