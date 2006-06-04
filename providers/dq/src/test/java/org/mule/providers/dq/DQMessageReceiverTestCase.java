/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.dq;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.mockobjects.dynamic.Mock;
import org.mule.MuleManager;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.providers.AbstractConnector;
import org.mule.tck.providers.AbstractMessageReceiverTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DQMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    public UMOMessageReceiver getMessageReceiver() throws Exception
    {
        getManager(true);
        Mock mockComponent = new Mock(UMOComponent.class);
        AS400 system = ((DQConnector)endpoint.getConnector()).getSystem();
        return new DQMessageReceiver((AbstractConnector) endpoint.getConnector(),
                                     (UMOComponent) mockComponent.proxy(),
                                     endpoint,
                                     new Long(1000),
                                     new DataQueue(system, "/QSYS.LIB/L701QUEUE.DTAQ"),
                                     system);
    }

    public UMOEndpoint getEndpoint() throws Exception {
        DQConnector c = new DQConnector();
        c.setName("dqConnector");
        c.setRecordFormat("DQ_recordFormat.xml");
        c.setUsername("xxx");
        c.setPassword("xxx");
        c.setHostname("localhost");
        c.initialise();
        MuleManager.getInstance().registerConnector(c);
        return new MuleEndpoint("dq://QSYS.LIB/L701QUEUE.DTAQ?createConnector=NEVER", true);
    }
}
