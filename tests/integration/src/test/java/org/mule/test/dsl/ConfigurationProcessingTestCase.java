/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.dsl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class ConfigurationProcessingTestCase extends FunctionalTestCase
{

    @Rule
    public SystemProperty frequency = new SystemProperty("frequency", "1000");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/dsl/parsing-test-config.xml";
    }

    @Test
    public void simpleFlowConfiguration() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("simpleFlow");
        assertThat(flow, notNullValue());
        assertThat(flow.getMessageProcessors(), notNullValue());
        assertThat(flow.getMessageProcessors().size(), is(1));
    }

    @Test
    public void complexFlowConfiguration() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("complexFlow");
        assertThat(flow, notNullValue());
        assertThat(flow.getMessageSource(), notNullValue());
        assertThat(flow.getMessageProcessors(), notNullValue());
        assertThat(flow.getMessageProcessors().size(), is(3));
    }
}
