/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.DbTypeManager;
import org.mule.module.db.internal.domain.type.JdbcTypes;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.module.db.test.util.DbConnectionBuilder;
import org.mule.module.db.test.util.DbTypeManagerBuilder;
import org.mule.module.db.test.util.ParameterMetaDataBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import org.junit.Test;

@SmallTest
public class QueryParamTypeResolverTestCase extends AbstractMuleTestCase
{

    private static final String SQL_TEXT = "select * from test where id = ?";
    private PreparedStatement preparedStatement;

    @Test
    public void resolvesQueryParameterKnownType() throws Exception
    {
        DbConnection connection = createDbConnection();

        DbTypeManager dbTypeManager = new DbTypeManagerBuilder().on(connection).managing(JdbcTypes.INTEGER_DB_TYPE).build();

        QueryTemplate queryTemplate = createQueryTemplate();

        QueryParamTypeResolver paramTypeResolver = new QueryParamTypeResolver(dbTypeManager);

        Map<Integer, DbType> parameterTypes = paramTypeResolver.getParameterTypes(connection, queryTemplate);

        assertThat(parameterTypes.size(), equalTo(1));
        assertThat(parameterTypes.get(1), equalTo(JdbcTypes.INTEGER_DB_TYPE));
        verify(preparedStatement).close();
    }

    @Test
    public void resolvesQueryParameterUnknownType() throws Exception
    {
        DbConnection connection = createDbConnection();

        QueryTemplate queryTemplate = createQueryTemplate();

        DbTypeManager dbTypeManager = new DbTypeManagerBuilder().on(connection).unknowing(JdbcTypes.INTEGER_DB_TYPE).build();

        QueryParamTypeResolver paramTypeResolver = new QueryParamTypeResolver(dbTypeManager);

        Map<Integer, DbType> parameterTypes = paramTypeResolver.getParameterTypes(connection, queryTemplate);

        assertThat(parameterTypes.size(), equalTo(1));
        assertThat(parameterTypes.get(1).getId(), equalTo(JdbcTypes.INTEGER_DB_TYPE.getId()));
        assertThat(parameterTypes.get(1).getName(), equalTo(JdbcTypes.INTEGER_DB_TYPE.getName()));
        verify(preparedStatement).close();
    }

    private DbConnection createDbConnection() throws SQLException
    {
        ParameterMetaData parameterMetaData = new ParameterMetaDataBuilder().withParameter(1, JdbcTypes.INTEGER_DB_TYPE).build();

        preparedStatement = mock(PreparedStatement.class);
        when(preparedStatement.getParameterMetaData()).thenReturn(parameterMetaData);

        return new DbConnectionBuilder().preparing(SQL_TEXT, preparedStatement).build();
    }

    private QueryTemplate createQueryTemplate()
    {
        return new QueryTemplate(SQL_TEXT, QueryType.SELECT, Collections.<QueryParam>singletonList(new DefaultInputQueryParam(1, UnknownDbType.getInstance(), "7", "param1")));
    }
}
