/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.sdk.api.annotation.Alias;

@Alias("sdk")
public class SdkPoolingTransactionalConnectionProvider extends SdkAbstractTransactionalConnectionProvider
    implements PoolingConnectionProvider<SdkTestTransactionalConnection> {

}
