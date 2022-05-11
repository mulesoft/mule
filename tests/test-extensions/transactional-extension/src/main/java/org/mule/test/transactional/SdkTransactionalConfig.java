/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.test.transactional.connection.SdkPoolingTransactionalConnectionProvider;

@Configuration(name = "sdk-config")
@ConnectionProviders({SdkPoolingTransactionalConnectionProvider.class})
@Operations(SdkTransactionalOperations.class)
@Sources({SdkTransactionalSource.class})
public class SdkTransactionalConfig {
}
