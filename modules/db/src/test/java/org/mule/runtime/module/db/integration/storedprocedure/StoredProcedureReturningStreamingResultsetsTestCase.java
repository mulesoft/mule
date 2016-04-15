/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.junit.Assume.assumeThat;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.matcher.SupportMultipleOpenResults;
import org.mule.module.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.runners.Parameterized;

public class StoredProcedureReturningStreamingResultsetsTestCase extends AbstractStoredProcedureReturningStreamingResultsetsTestCase
{

    public StoredProcedureReturningStreamingResultsetsTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return TestDbConfig.getResources();
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[] {"integration/storedprocedure/stored-procedure-streaming-returning-resultsets-config.xml"};
    }

    @Override
    public void setupStoredProcedure() throws Exception
    {
        assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
        assumeThat(getDefaultDataSource(), new SupportMultipleOpenResults());
        super.setupStoredProcedure();
    }
}
