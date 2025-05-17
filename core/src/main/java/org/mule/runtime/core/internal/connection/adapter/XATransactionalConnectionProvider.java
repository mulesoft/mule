/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection.adapter;

import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.sdk.api.connectivity.TransactionalConnection;

public interface XATransactionalConnectionProvider<C extends TransactionalConnection> extends ConnectionProvider<C> {

  PoolingProfile getXaPoolingProfile();

}
