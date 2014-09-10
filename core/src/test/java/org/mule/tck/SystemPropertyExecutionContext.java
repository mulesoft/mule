/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import java.util.HashMap;
import java.util.Map;

/**
 * Executes a test using the configured system properties.
 */
public class SystemPropertyExecutionContext
{

    Map<String, String> properties = new HashMap<String, String>();
    Map<String, String> propertiesOldValues = new HashMap<String, String>();

    public SystemPropertyExecutionContext addSystemProperty(String key, String value)
    {
        properties.put(key, value);
        return this;
    }

    public void execute(TestMethod testMethod) throws Exception
    {
        for (String key : properties.keySet())
        {
            propertiesOldValues.put(key, System.getProperty(key));
        }
        try
        {
            testMethod.execute();
        }
        finally
        {
            for (String key : propertiesOldValues.keySet())
            {
                final String oldValue = propertiesOldValues.get(key);
                if (oldValue == null)
                {
                    System.clearProperty(key);
                }
                else
                {
                    System.setProperty(key, oldValue);
                }
            }
        }
    }

    public interface TestMethod
    {
        void execute() throws Exception;
    }
}
