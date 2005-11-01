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
public class AxisConnectorJmsFunctionalTestCase extends AbstractSoapFunctionalTestCase
 {
    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis-jms-mule-config.xml";
    }

    String getProtocol() {
        return "axis";
    }

    protected String getRequestResponseEndpoint() {
        return "axis:jms://mycomponent?method=echo";
    }

    protected String getReceiveEndpoint() {
        return "axis:jms://mycomponent2?method=getDate";
    }

    protected String getReceiveComplexEndpoint() {
        return "axis:jms://mycomponent3?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1() {
        return "axis:jms://mycomponent3?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2() {
        return "axis:jms://mycomponent3?method=getPerson&param=Ross";
    }

    protected String getReceiveComplexCollectionEndpoint() {
        return "axis:jms://mycomponent3?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1() {
        return "axis:jms://mycomponent4?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2() {
        return "axis:jms://mycomponent3?method=getPerson&param=Joe";
    }

    protected String getTestExceptionEndpoint() {
        return "axis:jms://mycomponent3?method=addPerson";
    }
}
