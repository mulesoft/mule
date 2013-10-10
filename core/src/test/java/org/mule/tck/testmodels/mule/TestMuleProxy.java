/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.MuleException;
import org.mule.api.object.ObjectFactory;
import org.mule.component.DefaultJavaComponent;

/**
 * Makes the underlying POJO service object available for unit testing.
 */
public class TestMuleProxy extends DefaultJavaComponent
{

    public TestMuleProxy(ObjectFactory objectFactory) throws MuleException
    {
        super(objectFactory);
    }

    /** Returns the underlying POJO service object for unit testing. */
    public Object getPojoService() throws Exception
    {
        return objectFactory.getInstance(muleContext);
    }
}
