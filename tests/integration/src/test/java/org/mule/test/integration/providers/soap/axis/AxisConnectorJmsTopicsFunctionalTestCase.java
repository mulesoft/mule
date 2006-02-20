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
package org.mule.test.integration.providers.soap.axis;




/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisConnectorJmsTopicsFunctionalTestCase extends AxisConnectorJmsFunctionalTestCase
 {
    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis/axis-jms-topics-mule-config.xml";
    }

    public void testReceive() throws Throwable {
        //todo MULE20 once the resource info stuff is removed
    }

    public void testReceiveComplex() throws Throwable {
        //todo MULE20 once the resource info stuff is removed
    }

    public void testSendAndReceiveComplex() throws Throwable {
        //todo MULE20 once the resource info stuff is removed
    }

    public void testReceiveComplexCollection() throws Throwable {
        //todo MULE20 once the resource info stuff is removed
    }

    public void testDispatchAsyncComplex() throws Throwable {
        //todo MULE20 once the resource info stuff is removed
    }

    public void testException() throws Throwable {
        //todo MULE20 once the resource info stuff is removed
    }
}
