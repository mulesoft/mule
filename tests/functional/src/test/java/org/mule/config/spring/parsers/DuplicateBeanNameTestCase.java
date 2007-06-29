/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

public class DuplicateBeanNameTestCase extends AbstractBadConfigTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/duplicate-bean-name-test.xml";
    }

    public void testBeanError() throws Exception
    {
        assertErrorContains("Bean name 'child1' is already used in this file");
    }

}
