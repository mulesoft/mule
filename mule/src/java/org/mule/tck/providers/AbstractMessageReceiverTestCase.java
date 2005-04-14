/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.tck.providers;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * <code>AbstractMessageReceiver</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMessageReceiverTestCase extends AbstractMuleTestCase
{
    protected UMOComponent component;
    protected UMOEndpoint endpoint;
    protected UMOConnector connector;

    protected void setUp() throws Exception
    {
        connector = getTestConnector();
        component = getTestComponent(getTestDescriptor("orange", Orange.class.getName()));
        endpoint = getTestEndpoint("Test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
    }

    public void testCreate() throws Exception
    {
        UMOComponent component = getTestComponent(getTestDescriptor("orange", Orange.class.getName()));
        UMOEndpoint endpoint = getTestEndpoint("Test", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        UMOMessageReceiver receiver = getMessageReceiver();
        receiver.create(new TestConnector(), component, endpoint);

        assertNotNull(receiver.getEndpoint());
        assertNotNull(receiver.getConnector());

        try
        {
            receiver.setEndpoint(null);
            fail("Provider cannot be set to null");
        }
        catch (IllegalArgumentException e)
        { /* expected */
        }

        try
        {
            receiver.setComponent(null);
            fail("component cannot be set to null");
        }
        catch (IllegalArgumentException e)
        { /* expected */
        }

        receiver.setComponent(component);
        assertNotNull(receiver.getComponent());
        receiver.setEndpoint(endpoint);
        assertNotNull(receiver.getEndpoint());

        receiver.dispose();
    }

    public abstract UMOMessageReceiver getMessageReceiver() throws Exception;
}
