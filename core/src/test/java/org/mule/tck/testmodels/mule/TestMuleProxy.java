/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
