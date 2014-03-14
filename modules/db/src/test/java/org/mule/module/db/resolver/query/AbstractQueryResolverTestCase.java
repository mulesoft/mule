/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.resolver.query;

import static org.mockito.Mockito.mock;
import org.mule.api.MuleEvent;
import org.mule.module.db.domain.param.QueryParam;
import org.mule.module.db.domain.query.Query;
import org.mule.module.db.domain.query.QueryParamValue;
import org.mule.module.db.domain.query.QueryTemplate;
import org.mule.module.db.domain.query.QueryType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbstractQueryResolverTestCase extends AbstractMuleTestCase
{

    public static final String STATIC_SQL_TEXT = "select * from test";
    public static final String DYNAMIC_SQL_TEXT = "select * from #[table]";

    protected final MuleEvent muleEvent = mock(MuleEvent.class);

    protected Query createSelectQuery(String staticSqlText, Object... params)
    {
        QueryTemplate template = createSelectQueryTemplate(staticSqlText);
        List<QueryParamValue> paramValues = new ArrayList<QueryParamValue>();

        if (params != null)
        {
            int paramIndex = 1;
            for (Object param : params)
            {
                QueryParamValue paramValue = new QueryParamValue("param" + paramIndex++, param);
                paramValues.add(paramValue);
            }
        }

        return new Query(template, paramValues);
    }

    protected QueryTemplate createSelectQueryTemplate(String staticSqlText)
    {
        return new QueryTemplate(staticSqlText, QueryType.SELECT, Collections.<QueryParam>emptyList());
    }
}
