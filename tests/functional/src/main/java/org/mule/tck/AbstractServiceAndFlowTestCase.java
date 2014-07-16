/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.StringUtils;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@Deprecated
public abstract class AbstractServiceAndFlowTestCase extends FunctionalTestCase
{
    protected ConfigVariant variant;
    protected String configResources;

    public AbstractServiceAndFlowTestCase(ConfigVariant variant, String configResources)
    {
        super();
        this.variant = variant;
        this.configResources = configResources;
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        switch (variant)
        {
            case FLOW:
                doSetUpForFlow();
                break;

            case SERVICE:
                doSetUpForService();
                break;
        }
    }

    protected void doSetUpForFlow()
    {
        // subclasses can override this method with setup that is specific for the flow test variant
    }

    protected void doSetUpForService()
    {
        // subclasses can override this method with setup that is specific for the service test variant
    }

    @Override
    protected String getConfigFile()
    {
        if (configResources.contains(","))
        {
            return null;
        }
        return configResources;
    }
    
    @Override
    protected String[] getConfigFiles()
    {
        if (configResources.contains(","))
        {
            return StringUtils.splitAndTrim(configResources, ",");
        }
        return null;
    }

    @Override
    protected String getTestHeader()
    {
        return "Testing: " + name.getMethodName() + " (" + variant + ")";
    }

    public static enum ConfigVariant
    {
        FLOW, SERVICE, FLOW_EL
    }
}
