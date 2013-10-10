/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
