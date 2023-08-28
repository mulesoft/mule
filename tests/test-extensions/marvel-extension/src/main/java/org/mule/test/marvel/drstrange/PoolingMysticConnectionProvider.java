/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.drstrange;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.sdk.api.connectivity.PoolingConnectionProvider;

@Alias("pooling-mystic")
public class PoolingMysticConnectionProvider extends MysticConnectionProvider
    implements PoolingConnectionProvider<MysticConnection> {

}
