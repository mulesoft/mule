/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
