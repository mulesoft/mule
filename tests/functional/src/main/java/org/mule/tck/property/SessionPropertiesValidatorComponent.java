/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.property;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SessionPropertiesValidatorComponent implements Callable
{

    private Map<String,String> expectedProperties = new HashMap<String,String>();

    public void setExpectedProperties(Map<String, String> expectedProperties)
    {
        this.expectedProperties = expectedProperties;
    }

    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        if (expectedProperties.isEmpty())
        {
            throw new IllegalStateException("you must set at least one expected property");
        }
        for (String propertyName : expectedProperties.keySet())
        {
            assertThat((String) eventContext.getMessage().getSessionProperty(propertyName), is(expectedProperties.get(propertyName)));
        }
        return eventContext.getMessage();
    }
}
