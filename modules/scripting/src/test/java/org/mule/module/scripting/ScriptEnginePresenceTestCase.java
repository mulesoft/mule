/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;

import javax.script.ScriptEngineManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class ScriptEnginePresenceTestCase extends AbstractMuleTestCase
{

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {"groovy", "groovy"},
                {"jython", "py"},
                {"jruby", "rb"},
                {"rhino", "js"}
        });
    }

    @Parameter(0)
    public String engineName;

    @Parameter(1)
    public String extension;

    private ScriptEngineManager scriptEngineManager;

    @Before
    public void before()
    {
        scriptEngineManager = new ScriptEngineManager();
    }

    @Test
    public void findEngineByName() throws Exception
    {
        assertThat(scriptEngineManager.getEngineByName(engineName), notNullValue());
    }

    @Test
    public void findEngineByExtension() throws Exception
    {
        assertThat(scriptEngineManager.getEngineByExtension(extension), notNullValue());
    }
}
