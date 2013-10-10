/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class NameAndIdEqualMule2795TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/name-id-equal-mule-2795-test.xml";
    }

    @Test
    public void testNames()
    {
        assertNotNull(muleContext.getRegistry().lookupObject("id"));
        assertNull(muleContext.getRegistry().lookupObject(".:no-name"));
        assertNull(muleContext.getRegistry().lookupObject("org.mule.autogen.bean.1"));
        assertNotNull(muleContext.getRegistry().lookupObject("id2"));
        assertNull(muleContext.getRegistry().lookupObject(".:no-name-2"));
    }

}
