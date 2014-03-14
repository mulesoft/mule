/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.resolver.param;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.domain.query.QueryParamValue;
import org.mule.tck.size.SmallTest;

import java.util.List;

import org.junit.Test;

@SmallTest
public class StaticQueryParamResolverTestCase extends AbstractQueryParamResolverTestCase
{

    private final StaticQueryParamResolver paramResolver = new StaticQueryParamResolver();

    @Test
    public void resolvesStaticParam() throws Exception
    {
        List<QueryParamValue> templateParams = getQueryParamValues(777);

        List<QueryParamValue> resolvedParams = paramResolver.resolveParams(muleEvent, templateParams);

        assertThat(resolvedParams, equalTo(templateParams));
    }

    @Test
    public void returnsOriginalExpressionParam() throws Exception
    {
        List<QueryParamValue> templateParams = getQueryParamValues("#[payload]");

        List<QueryParamValue> resolvedParams = paramResolver.resolveParams(muleEvent, templateParams);

        assertThat(resolvedParams, equalTo(templateParams));
    }

    @Test
    public void resolveMultipleParams() throws Exception
    {
        List<QueryParamValue> templateParams = getQueryParamValues("#[payload]", 777);

        List<QueryParamValue> resolvedParams = paramResolver.resolveParams(muleEvent, templateParams);

        assertThat(resolvedParams, equalTo(templateParams));
    }
}
