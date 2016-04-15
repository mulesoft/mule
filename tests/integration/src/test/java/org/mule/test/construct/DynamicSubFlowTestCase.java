/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.api.message.NullPayload;

import org.junit.Test;

public class DynamicSubFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "dynamic-subflow-test-config.xml";
    }

    @Test
    public void testCofiguration() throws Exception
    {
        final MuleEvent muleEvent = flowRunner("ApplicationFlow").withPayload("").run();
        MuleMessage result = muleEvent.getMessage();
        assertThat(result, is(notNullValue()));
        assertThat(result.getExceptionPayload(), is(nullValue()));
        assertThat(result.getPayload(), not(instanceOf(NullPayload.class)));
    }
}
