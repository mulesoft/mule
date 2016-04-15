/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.statement;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.Collections;

import org.junit.Test;

@SmallTest
public class QueryStatementFactoryTestCase extends AbstractMuleTestCase
{

    @Test
    public void createsPreparedStatements() throws Exception
    {

        String sqlText = "call test";
        CallableStatement createdStatement = mock(CallableStatement.class);
        DbConnection connection = mock(DbConnection.class);
        when(connection.prepareCall(sqlText, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(createdStatement);

        QueryStatementFactory factory = new QueryStatementFactory();
        QueryTemplate queryTemplate = new QueryTemplate(sqlText, QueryType.STORE_PROCEDURE_CALL, Collections.<QueryParam>emptyList());
        CallableStatement statement = (CallableStatement) factory.create(connection, queryTemplate);

        assertThat(statement, is(createdStatement));
    }
}