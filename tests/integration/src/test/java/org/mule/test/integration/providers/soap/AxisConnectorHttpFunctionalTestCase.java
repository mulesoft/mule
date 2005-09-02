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

import org.mule.MuleManager;
import org.mule.config.ExceptionHelper;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;


/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisConnectorHttpFunctionalTestCase extends AbstractSoapFunctionalTestCase
{
    static public class ComponentWithoutInterfaces
    {
        public String echo(String msg)
        {
            return msg;
        }
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/axis-http-mule-config.xml";
    }

    protected String getComponentWithoutInterfacesEndpoint() {
        return "http://localhost:38011/mule/test";
    }
    protected String getRequestResponseEndpoint() {
        return "http://localhost:38008/mule/mycomponent?method=echo";
    }

    protected String getReceiveEndpoint() {
        return "http://localhost:38009/mule/services/mycomponent2?method=getDate";
    }

    protected String getReceiveComplexEndpoint() {
        return "http://localhost:38009/mycomponent3?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1() {
        return "http://localhost:38009/mycomponent3?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2() {
        return "http://localhost:38009/mycomponent3?method=getPerson&param=Ross";
    }

    protected String getReceiveComplexCollectionEndpoint() {
        return "http://localhost:38009/mycomponent3?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1() {
        return "http://localhost:38010/mycomponent4?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2() {
        return "http://localhost:38009/mycomponent3?method=getPerson&param=Joe";
    }

    protected String getTestExceptionEndpoint() {
        return "http://localhost:38009/mycomponent3?method=addPerson";
    }

    public void testComponentWithoutInterfaces() throws Throwable
    {
        try {
            UMOConnector c = ConnectorFactory.getConnectorByProtocol(getProtocol());
            UMODescriptor descriptor = new MuleDescriptor();
            descriptor.setExceptionListener(new DefaultExceptionStrategy());
            descriptor.setName("testComponentWithoutInterfaces");
            descriptor.setImplementation(ComponentWithoutInterfaces.class.getName());
            UMOEndpoint endpoint = new MuleEndpoint("testIn",
                                                    new MuleEndpointURI(getProtocol() + ":" + getComponentWithoutInterfacesEndpoint()),
                                                    c,
                                                    null,
                                                    UMOEndpoint.ENDPOINT_TYPE_RECEIVER,
                                                    0,
                                                    null);
            descriptor.setInboundEndpoint(endpoint);
            MuleManager.getInstance().getModel().registerComponent(descriptor);
            fail();
        } catch (UMOException e) {
            e = ExceptionHelper.getRootMuleException(e);
            assertTrue(e instanceof InitialisationException);
        }
    }
}
