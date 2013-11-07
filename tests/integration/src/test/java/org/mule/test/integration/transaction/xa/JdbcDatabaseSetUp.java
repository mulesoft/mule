/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

import org.mule.tck.util.MuleDerbyTestDatabase;

import org.junit.Ignore;

public class JdbcDatabaseSetUp implements TransactionalTestSetUp
{

    private final MuleDerbyTestDatabase muleDerbyTestDatabase;

    private JdbcDatabaseSetUp(String databaseNameProperty)
    {
        muleDerbyTestDatabase = new MuleDerbyTestDatabase(databaseNameProperty);
    }

    public static JdbcDatabaseSetUp createDatabaseOne()
    {
        return new JdbcDatabaseSetUp("database.name");
    }

    public static JdbcDatabaseSetUp createDatabaseTwo()
    {
        return new JdbcDatabaseSetUp("database.name2");
    }

    @Override
    public void initialize() throws Exception
    {
        muleDerbyTestDatabase.startDatabase();
    }

    @Override
    public void finalice() throws Exception
    {
        muleDerbyTestDatabase.stopDatabase();
    }

    public TransactionScenarios.InboundMessagesGenerator createInboundMessageCreator()
    {
        return new TransactionScenarios.InboundMessagesGenerator()
        {

            @Override
            public Integer generateInboundMessages() throws Exception
            {
                muleDerbyTestDatabase.emptyTestTable();
                for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
                {
                    muleDerbyTestDatabase.insertIntoTest("test " + i, 1);
                }
                return NUMBER_OF_MESSAGES;
            }
        };
    }

    public TransactionScenarios.OutboundMessagesCounter createOutboundMessageCreator()
    {
        return new TransactionScenarios.OutboundMessagesCounter()
        {
            @Override
            public int numberOfMessagesThatArrived() throws Exception
            {
                return muleDerbyTestDatabase.execSqlQuery("SELECT * FROM TEST WHERE TYPE = 2").size();
            }

            @Override
            public void close()
            {
            }
        };
    }
}
