/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;


public class SessionPropertiesWithMessageCollectionTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "session-properties-with-message-collection.xml";
    }

    @Test
    public void testSessionPropertyAfterSplitterAndAggregator() throws MuleException
    {
        final MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/test", TEST_MESSAGE, null);
        assertNotNullAndNotExceptionResponse(response);
    }

    private void assertNotNullAndNotExceptionResponse(MuleMessage response)
    {
        assertNotNull(response);
        if (response.getExceptionPayload() != null)
        {
            fail(response.getExceptionPayload().getException().getCause().toString());
        }
    }

    public static class TestSplitterComponent implements Callable
    {
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            ArrayList<String> elements = new ArrayList<String>();
            for(int index = 0; index < 5; index++)
            {
                elements.add("Element N" + index);
            }
            return elements;
        }
    }

}
