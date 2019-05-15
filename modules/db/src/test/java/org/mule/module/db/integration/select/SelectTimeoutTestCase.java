/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import org.junit.Rule;
import org.junit.Test;
import org.mule.module.db.integration.AbstractQueryTimeoutTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.tck.junit4.rule.SystemProperty;

public class SelectTimeoutTestCase extends AbstractQueryTimeoutTestCase
{

    private static final String QUERY_TIMEOUT_PARAMETER_FLOW = "queryTimeoutParameter";

    @Rule
    public SystemProperty queryTimeoutProperty = new SystemProperty("queryTimeoutParamValue", "1");

    public SelectTimeoutTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/select/select-timeout-config.xml"};
    }

    @Test
    public void timeoutsQueryParameterTest() throws Exception
    {
        timeoutsQuery(QUERY_TIMEOUT_PARAMETER_FLOW);
    }
}
