/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.common.TestResult;
import org.mule.common.Testable;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.module.db.integration.model.DerbyTestDatabase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.common.Result.Status.SUCCESS;

public class ConnectionTimeoutConfigTestCase extends AbstractDbIntegrationTestCase {

    @Rule
    public SystemProperty connectionTimeout = new SystemProperty("connectionTimeout", "10000");

    public ConnectionTimeoutConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
    {
        super(dataSourceConfigResource, testDatabase);
    }

    @Parameterized.Parameters
    public static List<Object[]> parameters()
    {
        return Collections.singletonList(new Object[]{"integration/config/connection-timeout-config.xml", new DerbyTestDatabase()});
    }

    @Override
    protected String[] getFlowConfigurationResources()
    {
        return new String[0];
    }

    @Test
    public void testsConnection() throws Exception
    {
        Testable testable = muleContext.getRegistry().lookupObject("dbConfig");

        TestResult result = testable.test();

        assertThat(result.getStatus(), equalTo(SUCCESS));
    }
}
