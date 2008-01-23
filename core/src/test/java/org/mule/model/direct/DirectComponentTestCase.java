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
import org.mule.model.AbstractComponentTestCase;
import org.mule.util.object.SingletonObjectFactory;

public class DirectComponentTestCase extends AbstractComponentTestCase
{

    protected void doSetUp() throws Exception
    {
        component = new DirectComponent();
        component.setName("direct");
        component.setServiceFactory(new SingletonObjectFactory(Object.class));
        component.setModel(new DirectModel());
        component.setMuleContext(muleContext);
    }

    protected void doTearDown() throws Exception
    {
        component = null;
    }

    public void testStop() throws MuleException
    {
        // TODO Remove this overridden empty implementation once MULE-2844 is resolved
    }

}
