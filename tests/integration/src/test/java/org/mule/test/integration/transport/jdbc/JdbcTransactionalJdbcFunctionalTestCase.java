/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.jdbc;

import org.junit.Ignore;
import org.mule.api.transaction.TransactionFactory;
import org.mule.transport.jdbc.JdbcTransactionFactory;

@Ignore("MULE-2749")
public class JdbcTransactionalJdbcFunctionalTestCase extends AbstractJdbcTransactionalFunctionalTestCase
{

    @Override
    protected TransactionFactory getTransactionFactory()
    {
        return new JdbcTransactionFactory();
    }

}
