/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.vendor.oracle;

import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.storedprocedure.AbstractStoredProcedureReturningStreamingResultsetsTestCase;

import java.util.List;

import org.junit.runners.Parameterized;

public class OracleStoredProcedureReturningStreamingResultsetsTestCase extends AbstractStoredProcedureReturningStreamingResultsetsTestCase
{

    public OracleStoredProcedureReturningStreamingResultsetsTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getOracleResource();
    }


    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/vendor/oracle/oracle-stored-procedure-returning-streaming-resultsets-config.xml"};
    }
}
