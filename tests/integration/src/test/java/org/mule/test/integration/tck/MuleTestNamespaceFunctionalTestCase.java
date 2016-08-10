/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.tck;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.util.ExceptionUtils;

import java.io.FileNotFoundException;

import org.junit.Test;

public class MuleTestNamespaceFunctionalTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/tck/test-namespace-config-flow.xml";
    }

    @Test
    public void testService1() throws Exception
    {
        MuleMessage message = flowRunner("testService1").withPayload("foo").run().getMessage();

        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertThat(getPayloadAsString(message), is("Foo Bar Car Jar"));
    }

    @Test
    public void testService2() throws Exception
    {
        MuleMessage message = flowRunner("testService2").withPayload("foo").run().getMessage();
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertThat(getPayloadAsString(message), is(loadResourceAsString("org/mule/test/integration/tck/test-data.txt")));
    }

    @Test
    public void testService3() throws Exception
    {
        MuleMessage message = flowRunner("testService3").withPayload("foo").run().getMessage();
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertThat(getPayloadAsString(message), is("foo received"));
    }

    @Test
    public void testService4() throws Exception
    {
        flowRunner("testService4").withPayload("foo").runExpectingException();
    }

    @Test
    public void testService5() throws Exception
    {
        MessagingException e = flowRunner("testService5").withPayload("foo").runExpectingException();
        assertTrue(ExceptionUtils.getRootCause(e) instanceof FileNotFoundException);
    }
}
