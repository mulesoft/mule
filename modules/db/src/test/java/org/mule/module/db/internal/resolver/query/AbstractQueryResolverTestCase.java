/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import static org.mockito.Mockito.mock;
import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.module.db.internal.domain.param.InputQueryParam;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class AbstractQueryResolverTestCase extends AbstractMuleTestCase
{

    public static final String STATIC_SQL_TEXT = "select * from test";
    public static final String DYNAMIC_SQL_TEXT = "select * from #[table]";

    protected final MuleEvent muleEvent = mock(MuleEvent.class);

    protected Query createQuery(QueryTemplate template, Object[] paramValues)
    {
        List<QueryParamValue> queryParamValues = new ArrayList<QueryParamValue>();

        if (paramValues != null)
        {
            int paramIndex = 1;
            for (Object param : paramValues)
            {
                QueryParamValue paramValue = new QueryParamValue("param" + paramIndex++, param);
                queryParamValues.add(paramValue);
            }
        }

        return new Query(template, queryParamValues);
    }

    protected Query createQuery(QueryTemplate template)
    {
        return new Query(template);
    }

    protected QueryTemplate createQueryTemplate(String staticSqlText)
    {
        return createQueryTemplate(staticSqlText, new DbType[0]);
    }

    protected QueryTemplate createQueryTemplate(String staticSqlText, DbType[] paramTypes)
    {

        List<QueryParam> queryParams = new ArrayList<QueryParam>(paramTypes.length);

        int index = 1;
        for (DbType paramType : paramTypes)
        {

            InputQueryParam inputQueryParam = new DefaultInputQueryParam(index, paramType, "param" + index);
            queryParams.add(inputQueryParam);
            index++;
        }

        return new QueryTemplate(staticSqlText, QueryType.SELECT, queryParams);
    }

}
