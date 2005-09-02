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
public class AxisConnectorVMFunctionalTestCase extends AbstractSoapFunctionalTestCase
 {
    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis-vm-mule-config.xml";
    }

    protected String getRequestResponseEndpoint() {
        return "vm://mycomponent?method=echo";
    }

    protected String getReceiveEndpoint() {
        return "vm://mycomponent2?method=getDate";
    }

    protected String getReceiveComplexEndpoint() {
        return "vm://mycomponent3?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1() {
        return "vm://mycomponent3?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2() {
        return "vm://mycomponent3?method=getPerson&param=Ross";
    }

    protected String getReceiveComplexCollectionEndpoint() {
        return "vm://mycomponent3?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1() {
        return "vm://mycomponent4?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2() {
        return "vm://mycomponent3?method=getPerson&param=Joe";
    }

    protected String getTestExceptionEndpoint() {
        return "vm://mycomponent3?method=addPerson";
    }
}
