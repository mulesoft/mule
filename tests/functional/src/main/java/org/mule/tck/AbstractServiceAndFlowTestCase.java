/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;


import org.junit.After;
import org.junit.Before;
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

    /**
     * We use JUnit 4 features in this subclass but our superclass is still JUnit 3 style. This
     * annotated method builds the bridge between the two worlds.
     */
    @Before
    public void before() throws Exception
    {
        setUp();
    }

    /**
     * We use JUnit 4 features in this subclass but our superclass is still JUnit 3 style. This
     * annotated method builds the bridge between the two worlds.
     */
    @After
    public void after() throws Exception
    {
        tearDown();
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

    public static enum ConfigVariant
    {
        FLOW, SERVICE
    }
}
