/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.resolver.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.module.db.domain.query.Query;
import org.mule.module.db.domain.query.QueryParamValue;
import org.mule.module.db.resolver.param.QueryParamResolver;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

@SmallTest
public class ParametrizedQueryResolverTestCase extends AbstractQueryResolverTestCase
{

    @Test
    public void returnsOriginalQueryWhenEventIsNull() throws Exception
    {
        Query query = createSelectQuery(STATIC_SQL_TEXT, null);

        QueryResolver queryResolver = new ParametrizedQueryResolver(query, null);

        Query resolvedQuery = queryResolver.resolve(null);

        assertThat(query, sameInstance(resolvedQuery));
    }

    @Test
    public void resolvesQuery() throws Exception
    {
        Query query = createSelectQuery(STATIC_SQL_TEXT, new Object[] {"foo"});

        QueryParamResolver queryParamResolver = mock(QueryParamResolver.class);
        QueryResolver queryResolver = new ParametrizedQueryResolver(query, queryParamResolver);

        List<QueryParamValue> resolvedParams = Collections.singletonList(new QueryParamValue("param1", "foo"));
        when(queryParamResolver.resolveParams(muleEvent, query.getParamValues())).thenReturn(resolvedParams);

        Query resolvedQuery = queryResolver.resolve(muleEvent);

        assertThat(query, not(sameInstance(resolvedQuery)));
        assertThat(query.getQueryTemplate(), sameInstance(resolvedQuery.getQueryTemplate()));
        assertThat(resolvedParams, sameInstance(resolvedQuery.getParamValues()));
        assertThat((String) resolvedParams.get(0).getValue(), equalTo("foo"));
    }
}
