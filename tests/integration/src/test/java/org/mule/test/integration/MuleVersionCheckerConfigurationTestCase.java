/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration;

import org.mule.api.config.MuleProperties;
import org.mule.api.config.MuleVersionChecker;
import org.mule.tck.junit4.FunctionalTestCase;

import junit.framework.Assert;

import org.junit.Test;

public class MuleVersionCheckerConfigurationTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/version-checker-test.xml";
    }

    /**
     * This test checks that the version checker is registered under the correct key
     */
    @Test
    public void testMuleVersionCheckerPresent()
    {
        MuleVersionChecker checker = muleContext.getRegistry().get(MuleProperties.MULE_VERSION_CHECKER);
        Assert.assertNotNull(
            "was expecting to find checker under key " + MuleProperties.MULE_VERSION_CHECKER, checker);
    }

}
