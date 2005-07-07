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
 *
 */
package org.mule.providers.soap;

import org.mule.tck.NamedTestCase;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.providers.soap.axis.AxisMessageDispatcher;
import org.mule.providers.soap.axis.AxisConnector;
import org.mule.umo.UMOEvent;
import org.mule.impl.endpoint.MuleEndpoint;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SoapActionTemplateTestCase extends AbstractMuleTestCase
{
    public void testHostInfoReplace() throws Exception {
        AxisMessageDispatcher dispatcher = new AxisMessageDispatcher(new AxisConnector());
        UMOEvent event = getTestEvent("test,", new MuleEndpoint("http://mycompany.com:8080/services/myService?method=foo", false));
        String result = dispatcher.parseSoapAction("${hostInfo}/${method}", "foo", event);

        assertEquals("http://mycompany.com:8080/foo", result);
    }

    public void testHostReplace() throws Exception {
        AxisMessageDispatcher dispatcher = new AxisMessageDispatcher(new AxisConnector());
        UMOEvent event = getTestEvent("test,", new MuleEndpoint("http://mycompany.com:8080/services/myService?method=foo", false));
        event.getComponent().getDescriptor().setName("myService");
        String result = dispatcher.parseSoapAction("${scheme}://${host}:${port}/${serviceName}/${method}", "foo", event);

        assertEquals("http://mycompany.com:8080/myService/foo", result);
    }
}
