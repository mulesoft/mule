/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.internal.domain.query.Query;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class StaticQueryResolverTestCase extends AbstractQueryResolverTestCase
{

    @Test
    public void resolvesStaticQuery() throws Exception
    {
        Query query = createQuery(createQueryTemplate(STATIC_SQL_TEXT));

        StaticQueryResolver queryResolver = new StaticQueryResolver(query);

        Query resolvedQuery = queryResolver.resolve(null, muleEvent);

        assertThat(query, equalTo(resolvedQuery));
    }
}
