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
package org.mule.test.integration.providers.soap;




/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisConnectorJmsTopicsFunctionalTestCase extends AbstractSoapFunctionalTestCase
 {
    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis-jms-topics-mule-config.xml";
    }

    String getProtocol() {
        return "axis";
    }

    protected String getRequestResponseEndpoint() {
        return "axis:jms://hello/mycomponent?method=echo&topic=true";
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

    protected String getReceiveEndpoint() {
        return "receiveEndpoint";
    }

    protected String getReceiveComplexEndpoint() {
        return "receiveComplexEndpoint";
    }

    protected String getSendReceiveComplexEndpoint1() {
        return "sendReceiveComplexEndpoint1";
    }

    protected String getSendReceiveComplexEndpoint2() {
        return "sendReceiveComplexEndpoint2";
    }

    protected String getReceiveComplexCollectionEndpoint() {
        return "receiveComplexCollectionEndpoint";
    }

    protected String getDispatchAsyncComplexEndpoint1() {
        return "dispatchAsyncComplexEndpoint1";
    }

    protected String getDispatchAsyncComplexEndpoint2() {
        return "dispatchAsyncComplexEndpoint2";
    }

    protected String getTestExceptionEndpoint() {
        return "axis:jms://mycomponent3?method=addPerson?topic=true";
    }
}
