/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.wsdl;

import org.apache.commons.pool.impl.StackObjectPool;
import org.mule.providers.soap.xfire.XFireMessageDispatcher;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * TODO document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireWsdlMessageDispatcher extends XFireMessageDispatcher
{

    public XFireWsdlMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }

    protected void doConnect(final UMOImmutableEndpoint endpoint) throws Exception
    {
        try {
            clientPool = new StackObjectPool(new XFireWsdlClientPoolFactory(endpoint, connector.getXfire()));
            clientPool.addObject();
        }
        catch (Exception ex) {
            disconnect();
            throw ex;
        }
    }

}
