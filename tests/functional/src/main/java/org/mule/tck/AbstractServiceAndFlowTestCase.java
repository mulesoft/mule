/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
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
    protected String getConfigResources()
    {
        return configResources;
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
