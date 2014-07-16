/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("Parameter override has to fail when a parameter name does not exist")
public class InvalidParamOverrideTestCase extends FunctionalTestCase
{

    private boolean errorDetected;

    public InvalidParamOverrideTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "integration/select/invalid-param-override-query-config.xml";
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        try
        {
            return super.createMuleContext();
        }
        catch (Exception e)
        {
            errorDetected = true;
            return null;
        }
    }

    @Test
    public void validatesInvalidParamOverride() throws Exception
    {
        assertTrue("Invalid parameter override should not be valid", errorDetected);
    }
}

