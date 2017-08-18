/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transactional;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.test.transactional.connection.NonPoolingTransactionalConnectionProvider;
import org.mule.test.transactional.connection.PoolingTransactionalConnectionProvider;

@Extension(name = "transactional")
@ConnectionProviders({PoolingTransactionalConnectionProvider.class, NonPoolingTransactionalConnectionProvider.class})
@Operations(TransactionalOperations.class)
@Sources({TransactionalSource.class, TransactionalSourceWithTXParameters.class})
@Xml(prefix = "tx")
@Export(classes = org.mule.test.transactional.TransactionalOperations.class)
public class TransactionalExtension {

}
