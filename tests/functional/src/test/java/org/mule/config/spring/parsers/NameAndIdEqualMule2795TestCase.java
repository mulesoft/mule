/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.tck.FunctionalTestCase;

public class NameAndIdEqualMule2795TestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/name-id-equal-mule-2795-test.xml";
    }

    public void testNames()
    {
        assertNotNull(managementContext.getRegistry().lookupObject("id"));
        assertNull(managementContext.getRegistry().lookupObject(".:no-name"));
        assertNull(managementContext.getRegistry().lookupObject("org.mule.autogen.bean.1"));
        assertNotNull(managementContext.getRegistry().lookupObject("id2"));
        assertNull(managementContext.getRegistry().lookupObject(".:no-name-2"));
    }

}
