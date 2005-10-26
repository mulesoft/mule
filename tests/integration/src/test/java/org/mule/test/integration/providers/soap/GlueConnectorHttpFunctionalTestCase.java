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
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueConnectorHttpFunctionalTestCase extends AxisConnectorHttpFunctionalTestCase
{
    static public class ComponentWithoutInterfaces
    {
        public String echo(String msg)
        {
            return msg;
        }
    }

    protected String getProtocol() {
        return "glue";
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/glue-http-mule-config.xml";
    }

    protected String getComponentWithoutInterfacesEndpoint() {
        return "glue:http://localhost:38011/mule/test";
    }
    protected String getRequestResponseEndpoint() {
        return "glue:http://localhost:38008/mule/mycomponent?method=echo";
    }

    protected String getReceiveEndpoint() {
        return "glue:http://localhost:38009/mule/services/mycomponent2?method=getDate";
    }

    protected String getReceiveComplexEndpoint() {
        return "glue:http://localhost:38009/mycomponent3?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1() {
        return "glue:http://localhost:38009/mycomponent3?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2() {
        return "glue:http://localhost:38009/mycomponent3?method=getPerson&param=Ross";
    }

    protected String getReceiveComplexCollectionEndpoint() {
        return "glue:http://localhost:38009/mycomponent3?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1() {
        return "axis:http://localhost:38010/mycomponent4?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2() {
        return "glue:http://localhost:38009/mycomponent3?method=getPerson&param=Joe";
    }

    protected String getTestExceptionEndpoint() {
        return "glue:http://localhost:38009/mycomponent3?method=addPerson";
    }
}
