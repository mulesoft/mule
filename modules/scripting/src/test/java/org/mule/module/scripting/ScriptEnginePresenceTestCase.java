/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@SmallTest
@RunWith(Parameterized.class)
public class ScriptEnginePresenceTestCase extends AbstractMuleTestCase
{

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                                             {"groovy", "groovy", "Groovy Scripting Engine", "2.0", "return 10"},
                                             {"jython", "py", "jython", "2.7.1", "print \"hello world\""},
                                             {"jruby", "rb", "JSR 223 JRuby Engine", "9.2.7.0", "1.+ 2"},
                                             {"rhino", "js", "Mozilla Rhino", "1.6R5", "print(\"hello world\");"},
                                             {"rhino", "js", "Mozilla Rhino", "1.6 release 2", "print(\"hello world\");"}
        });
    }

    @Parameter(0)
    public String engineName;

    @Parameter(1)
    public String extension;

    @Parameter(2)
    public String fullName;

    @Parameter(3)
    public String version;

    @Parameter(4)
    public String scriptCode;

    private ScriptEngineManager scriptEngineManager;

    @Before
    public void before()
    {
        scriptEngineManager = new ScriptEngineManager();
    }

    @Test
    public void allEngines()
    {
        List<ScriptEngineFactory> engineFactories = scriptEngineManager.getEngineFactories();
        for (ScriptEngineFactory scriptEngineFactory : engineFactories)
        {
            scriptEngineFactory.getScriptEngine();
        }
        assertThat(engineFactories, hasItem(new TypeSafeMatcher<ScriptEngineFactory>()
        {
            @Override
            public void describeTo(Description description)
            {
                description.appendText("expected '" + fullName + " " + version + "'");
            }

            @Override
            protected void describeMismatchSafely(ScriptEngineFactory item, Description mismatchDescription)
            {
                mismatchDescription.appendText("was ").appendValue("'" + item.getEngineName() + " " + item.getEngineVersion() + "'");
            }

            @Override
            protected boolean matchesSafely(ScriptEngineFactory item)
            {
                return fullName.equals(item.getEngineName()) && version.equals(item.getEngineVersion());
            }
        }));
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

    @Test
    public void runTestScript() throws Exception {
        ScriptEngine engine = scriptEngineManager.getEngineByName(engineName);
        Bindings bindings = engine.createBindings();

        engine.eval(scriptCode, bindings);
    }
}
