/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class NameAndIdEqualMule2795TestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
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
