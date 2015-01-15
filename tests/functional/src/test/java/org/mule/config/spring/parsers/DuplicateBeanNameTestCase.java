/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers;

import org.junit.Test;

public class DuplicateBeanNameTestCase extends AbstractBadConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/duplicate-bean-name-test.xml";
    }

    @Test
    public void testBeanError() throws Exception
    {
        assertErrorContains("Bean name 'child1' is already used in this <beans> element");
    }

}
