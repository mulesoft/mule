/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.external.applications;

public class TestServiceImpl implements TestService
{
    private Test[] tests = new Test[] {};

    public TestServiceImpl()
    {
        tests = new Test[] { new Test("test1"), new Test("test2") };
    }

    public Test[] getTests()
    {
        return tests;
    }

    public Test getTest(String key) throws Exception
    {
        for (int i = 0; i < tests.length; i++)
        {
            if (tests[i].getKey().equals(key)) return tests[i];
        }

        throw new Exception("No test found with key " + key);
    }

}
