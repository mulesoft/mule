/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.internal.domain.connection.DbConnection;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.module.db.internal.domain.type.JdbcTypes;
import org.mule.module.db.internal.domain.type.UnknownDbType;
import org.mule.module.db.internal.resolver.param.ParamValueResolver;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

@SmallTest
public class ParametrizedQueryResolverTestCase extends AbstractQueryResolverTestCase
{

    private final Query resolvedTemplateQuery = createQuery(createQueryTemplate(STATIC_SQL_TEXT, new DbType[] {JdbcTypes.INTEGER_DB_TYPE}), new Object[] {"foo"});
    private final Query unresolvedTemplateQuery = createQuery(createQueryTemplate(STATIC_SQL_TEXT, new DbType[] {UnknownDbType.getInstance()}), new Object[] {"foo"});

    @Test
    public void resolvesQuery() throws Exception
    {
        ParamValueResolver paramValueResolver = mock(ParamValueResolver.class);
        QueryResolver queryResolver = new ParametrizedQueryResolver(resolvedTemplateQuery, paramValueResolver);

        List<QueryParamValue> resolvedParams = Collections.singletonList(new QueryParamValue("param1", "foo"));
        when(paramValueResolver.resolveParams(muleEvent, resolvedTemplateQuery.getParamValues())).thenReturn(resolvedParams);

        Query resolvedQuery = queryResolver.resolve(null, muleEvent);

        assertThat(resolvedTemplateQuery, not(sameInstance(resolvedQuery)));
        assertThat(resolvedTemplateQuery.getQueryTemplate(), sameInstance(resolvedQuery.getQueryTemplate()));
        assertThat(resolvedParams, sameInstance(resolvedQuery.getParamValues()));
        assertThat((String) resolvedParams.get(0).getValue(), equalTo("foo"));
    }

    @Test
    public void resolvesQueryWithUnresolvedTemplate() throws Exception
    {
        ParamValueResolver paramValueResolver = mock(ParamValueResolver.class);
        List<QueryParamValue> resolvedParams = Collections.singletonList(new QueryParamValue("param1", "foo"));
        when(paramValueResolver.resolveParams(muleEvent, unresolvedTemplateQuery.getParamValues())).thenReturn(resolvedParams);

        QueryResolver queryResolver = new ParametrizedQueryResolver(unresolvedTemplateQuery, paramValueResolver);

        DbConnection connection = mock(DbConnection.class);
        when(connection.getParamTypes(unresolvedTemplateQuery.getQueryTemplate())).thenReturn(Collections.singletonMap(1, JdbcTypes.INTEGER_DB_TYPE));

        Query resolvedQuery = queryResolver.resolve(connection, muleEvent);

        assertThat(unresolvedTemplateQuery, not(sameInstance(resolvedQuery)));
        assertThat(unresolvedTemplateQuery.getQueryTemplate().getSqlText(), equalTo(resolvedQuery.getQueryTemplate().getSqlText()));
        assertThat(resolvedQuery.getQueryTemplate().getParams().get(0).getType(), equalTo(JdbcTypes.INTEGER_DB_TYPE));
        assertThat(resolvedParams, sameInstance(resolvedQuery.getParamValues()));
        assertThat((String) resolvedParams.get(0).getValue(), equalTo("foo"));
    }
}
