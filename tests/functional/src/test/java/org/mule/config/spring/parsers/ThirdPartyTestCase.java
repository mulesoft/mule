/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.parsers.beans.ThirdPartyContainer;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ThirdPartyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
