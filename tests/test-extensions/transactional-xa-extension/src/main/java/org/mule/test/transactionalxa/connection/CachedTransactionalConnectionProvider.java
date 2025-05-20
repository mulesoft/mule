/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactionalxa.connection;

import org.mule.sdk.api.annotation.Alias;
import org.mule.sdk.api.connectivity.CachedConnectionProvider;

@Alias("cached")
public class CachedTransactionalConnectionProvider extends AbstractTransactionalConnectionProvider
    implements CachedConnectionProvider<TestTransactionalConnection> {



}
