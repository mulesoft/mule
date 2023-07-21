/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
