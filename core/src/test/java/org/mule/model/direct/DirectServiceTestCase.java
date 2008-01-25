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

import org.mule.api.MuleException;
import org.mule.model.AbstractServiceTestCase;
import org.mule.util.object.SingletonObjectFactory;

public class DirectServiceTestCase extends AbstractServiceTestCase
{

    protected void doSetUp() throws Exception
    {
        service = new DirectService();
        service.setName("direct");
        service.setServiceFactory(new SingletonObjectFactory(Object.class));
        service.setModel(new DirectModel());
        service.setMuleContext(muleContext);
    }

    protected void doTearDown() throws Exception
    {
        service = null;
    }

    public void testStop() throws MuleException
    {
        // TODO Remove this overridden empty implementation once MULE-2844 is resolved
    }

}
