/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import static org.junit.Assert.assertNotNull;

import org.mule.config.spring.parsers.beans.ThirdPartyContainer;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ThirdPartyTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/third-party-test.xml";
    }

    @Test
    public void testContainer()
    {
        ThirdPartyContainer container = (ThirdPartyContainer) muleContext.getRegistry().lookupObject("container");
        assertNotNull(container);
        assertNotNull(container.getThing());
    }
}
