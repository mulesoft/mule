/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.param;

import static org.mockito.Mockito.mock;
import org.mule.api.MuleEvent;
import org.mule.module.db.internal.domain.query.QueryParamValue;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class AbstractQueryParamResolverTestCase extends AbstractMuleTestCase
{

    protected final MuleEvent muleEvent = mock(MuleEvent.class);

    protected List<QueryParamValue> getQueryParamValues(Object... values)
    {
        List<QueryParamValue> paramValues = new ArrayList<QueryParamValue>();
        int paramIndex =1;
        for (Object value : values)
        {
            QueryParamValue paramValue = new QueryParamValue("param" + paramIndex++, value);
            paramValues.add(paramValue);
        }

        return paramValues;
    }
}
