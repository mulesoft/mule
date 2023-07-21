/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.test.transactional.connection.NonPoolingTransactionalConnectionProvider;
import org.mule.test.transactional.connection.PoolingTransactionalConnectionProvider;

@Configuration(name = "config")
@ConnectionProviders({PoolingTransactionalConnectionProvider.class, NonPoolingTransactionalConnectionProvider.class})
@Operations(TransactionalOperations.class)
@Sources({TransactionalSource.class, TransactionalSourceWithTXParameters.class})
public class TransactionalConfig {
}
