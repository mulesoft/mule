/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.MuleUrlStreamHandlerFactory;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import static org.junit.Assert.assertEquals;

import java.util.Map;

public class SRubyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "jruby-config.xml";
    }

    @Rule
    public SystemProperty jRubyGlobalVar =
            new SystemProperty("org.jruby.embed.localvariable.behavior", "global");

    @Rule
    public TestRule globalTimeout = createTestTimeoutRule();

    private final String key = "message";
    private final String value = "myMessage";

    @Before
    public void setup() {
        setDisposeContextPerClass(false);
    }

    @Test
    public void testJRuby() throws Exception
    {
        System.out.println("system org.jruby.embed.localvariable.behavior:-" + System.getProperty("org.jruby.embed.localvariable.behavior"));
        MuleEvent event = getTestEvent(key);
        event.setFlowVariable(key, value);

        MuleEvent response = runFlow("rubyTestFlow", event);
        assertEquals(response.getMessage().getPayloadAsString(), value);
    }
}
