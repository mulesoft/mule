/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.config.domain.query;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.internal.domain.query.QueryType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DynamicQueryTemplateFactoryBeanTestCase extends AbstractMuleTestCase
{

    @Test
    public void createsDynamicQuery() throws Exception
    {
        String dynamicQuery = "select from #[table]";

        DynamicQueryTemplateFactoryBean factoryBean = new DynamicQueryTemplateFactoryBean(dynamicQuery);

        QueryTemplate queryTemplate = factoryBean.getObject();

        assertThat(queryTemplate.getSqlText(), equalTo(dynamicQuery));
        assertThat(queryTemplate.getType(), equalTo(QueryType.DDL));
        assertThat(queryTemplate.getParams(), is(empty()));
    }
}