/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.config;

import org.mule.tck.AbstractConfigurationErrorTestCase;

import org.junit.Test;

public class OracleMissingUserTestCase extends AbstractConfigurationErrorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "integration/config/oracle-missing-user-db-config.xml";
    }

    @Test
    public void requiresUserAttribute() throws Exception
    {
        assertConfigurationError("Able to define an Oracle config without user attribute", "user");
    }
}
