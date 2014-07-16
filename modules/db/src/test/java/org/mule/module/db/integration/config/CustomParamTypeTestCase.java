/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.module.db.internal.domain.param.QueryParam;
import org.mule.module.db.internal.domain.query.QueryTemplate;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class CustomParamTypeTestCase extends AbstractDbIntegrationTestCase
{

    public CustomParamTypeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getDerbyResource();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/config/custom-param-type-config.xml"};
    }

    @Test
    public void usesCustomType() throws Exception
    {
        QueryTemplate parameterizedQueryTemplate = muleContext.getRegistry().lookupObject("parameterizedQuery");
        QueryParam queryParam = parameterizedQueryTemplate.getParams().get(0);

        assertThat(queryParam.getType().getName(), equalTo("CUSTOM_TYPE1"));
    }
}