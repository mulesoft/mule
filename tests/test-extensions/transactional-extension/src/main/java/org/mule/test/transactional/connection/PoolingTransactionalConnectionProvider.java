/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.api.connection.PoolingConnectionProvider;

public class PoolingTransactionalConnectionProvider extends AbstractTransactionalConnectionProvider
    implements PoolingConnectionProvider<TestTransactionalConnection> {

}
