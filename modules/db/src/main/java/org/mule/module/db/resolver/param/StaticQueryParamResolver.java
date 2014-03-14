/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.resolver.param;

import org.mule.api.MuleEvent;
import org.mule.module.db.domain.query.QueryParamValue;

import java.util.List;

/**
 * Resolves a query parameters to a static value without using the current event
 */
public class StaticQueryParamResolver implements QueryParamResolver
{

    @Override
    public List<QueryParamValue> resolveParams(MuleEvent muleEvent, List<QueryParamValue> templateParams)
    {
        return templateParams;
    }
}
