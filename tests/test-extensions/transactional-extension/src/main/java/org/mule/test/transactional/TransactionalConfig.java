/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
