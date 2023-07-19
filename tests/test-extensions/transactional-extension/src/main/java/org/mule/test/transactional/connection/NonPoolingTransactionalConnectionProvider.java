/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transactional.connection;

import org.mule.runtime.extension.api.annotation.Alias;

@Alias("non-pooling")
public class NonPoolingTransactionalConnectionProvider extends AbstractTransactionalConnectionProvider {

}
