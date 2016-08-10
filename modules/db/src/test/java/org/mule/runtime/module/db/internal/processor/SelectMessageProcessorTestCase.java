/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.internal.processor;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.internal.domain.connection.DbConnection;
import org.mule.runtime.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.runtime.module.db.internal.domain.executor.SelectExecutor;
import org.mule.runtime.module.db.internal.domain.param.QueryParam;
import org.mule.runtime.module.db.internal.domain.query.Query;
import org.mule.runtime.module.db.internal.domain.query.QueryTemplate;
import org.mule.runtime.module.db.internal.domain.query.QueryType;
import org.mule.runtime.module.db.internal.domain.transaction.TransactionalAction;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Matchers;

@Ignore("Re-add query validation")
@SmallTest
public class SelectMessageProcessorTestCase extends AbstractMuleTestCase
{

    @Test
    public void testAcceptsValidQuery() throws Exception
    {
        // Implement
    }

    @Test
    public void testRejectsNonSupportedSql() throws Exception
    {
        DbConnectionFactory dbConnectionFactory = mock(DbConnectionFactory.class);

        for (QueryType type : QueryType.values())
        {
            QueryTemplate queryTemplate = new QueryTemplate("UNUSED SQL TEXT", type, Collections.<QueryParam>emptyList());

            if (type != QueryType.SELECT && type != QueryType.STORE_PROCEDURE_CALL)
            {
                try
                {
                    Query query = new Query(queryTemplate, null);
                    new SelectMessageProcessor(null, null, null, null, false);
                    fail("SelectMessageProcessor should accept SELECT and DYNAMIC queries only");
                }
                catch (IllegalArgumentException expected)
                {
                }
            }
        }
    }

    @Test
    public void testCommitsWorkIfNoTransactionDefined() throws Exception
    {
        DbConnection connection = mock(DbConnection.class);
        DbConnectionFactory dbConnectionFactory = mock(DbConnectionFactory.class);
        when(dbConnectionFactory.createConnection(TransactionalAction.JOIN_IF_POSSIBLE)).thenReturn(connection);
        SelectExecutor selectExecutor = mock(SelectExecutor.class);

        SelectMessageProcessor processor = new SelectMessageProcessor(null, null, null, null, false);
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage muleMessage = mock(MuleMessage.class);
        when(event.getMessage()).thenReturn(muleMessage);

        processor.process(event);
        verify(selectExecutor, times(1)).execute(eq(connection), Matchers.any(Query.class));
        verify(dbConnectionFactory, times(1)).releaseConnection(connection);
    }
}
