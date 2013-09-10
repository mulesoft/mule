/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class DuplicateBeanNameTestCase extends AbstractBadConfigTestCase
{

    public DuplicateBeanNameTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);     
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/config/spring/parsers/duplicate-bean-name-test.xml"},            
        });
    }      

    @Test
    public void testBeanError() throws Exception
    {
        assertErrorContains("Bean name 'child1' is already used in this <beans> element");
    }

}
