/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.config.PropertyFactory;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.Map;

/**
 * <code>TestDependentObject</code> is used as a mock dependency for an object
 */
public class TestDependentObject implements PropertyFactory
{
    
    public Object create(Map<?, ?> properties) throws Exception
    {
        // make sure that both test properties are set here
        if (properties.get("test1") == null || properties.get("test2") == null)
        {
            throw new Exception("Both properties should be set before the factory method is called");
        }
        return new Orange();
    }
    
}
