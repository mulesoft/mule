/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.lifecycle.Callable;
import org.mule.runtime.core.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

@Ignore("Session properties are not supported anymore")
public class SessionPropertiesWithMessageCollectionTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "session-properties-with-message-collection.xml";
    }

    @Test
    public void sessionPropertyAfterSplitterAndAggregator() throws MuleException
    {
        final MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/test", TEST_MESSAGE, null);
        assertNotNullAndNotExceptionResponse(response);
    }

    @Test
    public void splitterAndAggregatorWithPersistentStore() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("synchronousCollectionAggregatorFlow");
        List<String> inputData = new ArrayList<String>();
        int numberOfElements = 10;
        for (int i = 0; i < numberOfElements; i++)
        {
            inputData.add(String.valueOf(i));
        }
        MuleEvent responseEvent = flow.process(getTestEvent(inputData));
        assertThat(responseEvent.getSession().<List>getProperty("recordsToUpdate").size(), is(numberOfElements));
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
        @Override
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
