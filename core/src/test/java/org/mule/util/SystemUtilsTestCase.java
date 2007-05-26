/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SystemUtilsTestCase extends AbstractMuleTestCase
{

    public void testEnvironment() throws Exception
    {
        Map env = SystemUtils.getenv();
        assertNotNull(env);
        assertFalse(env.isEmpty());
        assertSame(env, SystemUtils.getenv());

        String envVarToTest = (SystemUtils.IS_OS_WINDOWS ? "Path" : "PATH");
        // This is a hack to catch Cygwin environments; it won't work in cases where
        // the user has a different term from /etc/termcaps
        if (SystemUtils.IS_OS_WINDOWS)
        {
            String term = (String) env.get("TERM");
            if (term != null && term.equals("cygwin")) envVarToTest = "PATH";
        }

        assertNotNull(env.get(envVarToTest));
    }

    public void testParsePropertyDefinitions()
    {
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions(null));
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions(""));
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions(" "));
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("foo"));
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("-D"));
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("=-D"));
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("=foo"));
        assertEquals(Collections.EMPTY_MAP, SystemUtils.parsePropertyDefinitions("-D=-Dfoo-D=="));

        Map expected = MapUtils.mapWithKeysAndValues(HashMap.class, new String[]{"keyOnly", "mule.foo",
            "mule.bar"}, new String[]{"true", "true", "false"});

        String input = "  standalone key=value -D -D= -DkeyOnly -D=noKey -Dmule.foo=true -Dmule.bar=false ";

        assertEquals(expected, SystemUtils.parsePropertyDefinitions(input));
    }

}
