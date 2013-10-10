/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.module.management.mbean.YourKitProfilerServiceMBean;

import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class YourKitProfilerTestCase extends AbstractMuleJmxTestCase
{

    /**
     * Each method doen't have to be more than 32
     * There is a need for intergration with HQ
     *
     * @throws Exception
     */
    @Test
    public void testMethodLenght() throws Exception
    {
        Method[] methods = YourKitProfilerServiceMBean.class.getMethods();
        for (int i = 0; i < methods.length; i++)
        {
            assertTrue(methods[i].getName(),methods[i].getName().length() < 32);
        }

    }

}
