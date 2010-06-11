/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model.direct;

import org.mule.api.service.Service;
import org.mule.component.DefaultJavaComponent;
import org.mule.model.AbstractServiceTestCase;
import org.mule.object.SingletonObjectFactory;

public class DirectServiceTestCase extends AbstractServiceTestCase
{
    private DirectService service;

    protected void doSetUp() throws Exception
    {
        service = new DirectService(muleContext);
        service.setName("direct");
        SingletonObjectFactory factory = new SingletonObjectFactory(Object.class);
        final DefaultJavaComponent component = new DefaultJavaComponent(factory);
        component.setMuleContext(muleContext);
        service.setComponent(component);
        service.setModel(new DirectModel());
    }

    @Override
    protected Service getService()
    {
        return service;
    }
}
