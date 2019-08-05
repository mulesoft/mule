/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;


/**
 * This is the main class of an extension, is the entry point from which configurations, connection providers, operations
 * and sources are going to be declared.
 */
@Extension(name = "reconnection")
@Configurations(ReconnectionConfiguration.class)
public class ReconnectionExtension {

}
