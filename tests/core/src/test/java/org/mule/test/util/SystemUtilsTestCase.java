/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util;

import java.util.Map;

import junit.framework.TestCase;

import org.mule.util.SystemUtils;

public class SystemUtilsTestCase extends TestCase
{

    public void testEnvironment() throws Exception
    {
        Map env = SystemUtils.getenv();
        assertNotNull(env);
        assertFalse(env.isEmpty());

        String envVarToTest = (SystemUtils.IS_OS_WINDOWS ? "Path" : "PATH");
        assertNotNull(env.get(envVarToTest));
    }

}
