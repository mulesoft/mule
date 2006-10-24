/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class OracleJmsEndpointTestCase extends AbstractMuleTestCase
{
    public void testWithoutPayloadFactory() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://XML_QUEUE?transformers=XMLMessageToString");
        assertNull(url.getParams().getProperty(OracleJmsConnector.PAYLOADFACTORY_PROPERTY));
    }

    public void testWithPayloadFactory() throws Exception
    {
        UMOEndpointURI url = new MuleEndpointURI("jms://XML_QUEUE" + "?"
                                                 + OracleJmsConnector.PAYLOADFACTORY_PROPERTY
                                                 + "=oracle.xdb.XMLTypeFactory"
                                                 + "&transformers=XMLMessageToString");
        assertEquals("oracle.xdb.XMLTypeFactory", url.getParams().getProperty(
            OracleJmsConnector.PAYLOADFACTORY_PROPERTY));
    }
}
