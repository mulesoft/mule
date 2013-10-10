/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
