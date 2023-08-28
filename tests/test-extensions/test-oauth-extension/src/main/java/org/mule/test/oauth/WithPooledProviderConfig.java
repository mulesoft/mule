/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.sdk.api.annotation.connectivity.ConnectionProviders;

@ConnectionProviders(TestOAuthPooledProvider.class)
@Operations(TestOAuthOperations.class)
@Configuration(name = "pooled-provider")
public class WithPooledProviderConfig {

}
