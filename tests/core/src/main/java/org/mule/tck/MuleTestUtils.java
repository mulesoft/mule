/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.MuleManager;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.RequestContext;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.impl.model.seda.SedaModel;
import org.mule.routing.outbound.OutboundPassThroughRouter;
import org.mule.tck.testmodels.mule.TestAgent;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOSession;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;

import com.mockobjects.dynamic.Mock;

import java.util.HashMap;

/**
 * Utilities for creating test and Mock Mule objects
 */
public class MuleTestUtils
{

    public static final String DEFAULT_MODEL_NAME = "main";

    public static UMOManager getManager(boolean disableAdminService) throws Exception
    {
        if (MuleManager.isInstanciated())
        {
            MuleManager.getInstance().dispose();
        }

        UMOManager manager = MuleManager.getInstance();

        UMOModel model = new SedaModel();
        model.setName(DEFAULT_MODEL_NAME);
        manager.registerModel(model);
        MuleManager.getConfiguration().setDefaultSynchronousEndpoints(true);
        
        return manager;
    }

    public static UMOModel getDefaultModel() throws UMOException
    {
        UMOModel m = MuleManager.getInstance().lookupModel(DEFAULT_MODEL_NAME);
        if(m==null)
        {
            m = new SedaModel();
            m.setName(DEFAULT_MODEL_NAME);
            MuleManager.getInstance().registerModel(m);
        }
        return m;

    }

    public static UMOEndpoint getTestEndpoint(String name, String type) throws Exception
    {
        UMOEndpoint endpoint = new MuleEndpoint();
        // need to build endpoint this way to avoid depenency to any endpoint jars
        UMOConnector connector = null;
        connector = (UMOConnector)ClassUtils.loadClass("org.mule.tck.testmodels.mule.TestConnector",
            AbstractMuleTestCase.class).newInstance();

        connector.setName("testConnector");
        endpoint.setConnector(connector);
        endpoint.setEndpointURI(new MuleEndpointURI("test://test"));
        endpoint.setName(name);
        endpoint.setType(type);
        return endpoint;
    }

    public static UMOEvent getTestEvent(Object data) throws Exception
    {
        UMOComponent component = getTestComponent(getTestDescriptor("string", String.class.getName()));
        UMOSession session = getTestSession(component);
        return new MuleEvent(new MuleMessage(data, new HashMap()), getTestEndpoint("test1",
            UMOEndpoint.ENDPOINT_TYPE_SENDER), session, true);
    }

    public static UMOEventContext getTestEventContext(Object data) throws Exception
    {
        try
        {
            UMOEvent event = getTestEvent(data);
            RequestContext.setEvent(event);
            return RequestContext.getEventContext();
        }
        finally
        {
            RequestContext.setEvent(null);
        }
    }

    public static UMOTransformer getTestTransformer()
    {
        return new TestCompressionTransformer();
    }

    public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor) throws Exception
    {
        UMOComponent component = getTestComponent(descriptor);

        UMOSession session = getTestSession(component);

        UMOEndpoint endpoint = getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        return new MuleEvent(new MuleMessage(data, new HashMap()), endpoint, session, true);
    }

    public static UMOEvent getTestEvent(Object data, UMOImmutableEndpoint endpoint) throws Exception
    {
        UMOSession session = getTestSession(getTestComponent(getTestDescriptor("string", String.class
            .getName())));
        return new MuleEvent(new MuleMessage(data, new HashMap()), endpoint, session, true);
    }

    public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor, UMOImmutableEndpoint endpoint)
        throws UMOException
    {
        UMOSession session = getTestSession(getTestComponent(descriptor));
        UMOEvent event = new MuleEvent(new MuleMessage(data, new HashMap()), endpoint, session, true);
        return event;
    }

    public static UMOSession getTestSession(UMOComponent component)
    {
        return new MuleSession(component);
    }

    public static UMOSession getTestSession()
    {
        return new MuleSession(null);
    }

    public static TestConnector getTestConnector()
    {
        TestConnector testConnector = new TestConnector();
        testConnector.setName("testConnector");
        return testConnector;
    }

    public static UMOComponent getTestComponent(MuleDescriptor descriptor)
    {
        return new SedaComponent(descriptor, new SedaModel());
    }

    public static MuleDescriptor getTestDescriptor(String name, String implementation) throws Exception
    {
        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setExceptionListener(new DefaultExceptionStrategy());
        descriptor.setName(name);
        descriptor.setImplementation(implementation);
        UMOOutboundRouter router = new OutboundPassThroughRouter();
        router.addEndpoint(getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER));
        descriptor.getOutboundRouter().addRouter(router);
        descriptor.initialise();

        return descriptor;
    }


    public static TestAgent getTestAgent()
    {
        return new TestAgent();
    }

    public static Mock getMockSession()
    {
        return new Mock(UMOSession.class, "umoSession");
    }

    public static Mock getMockMessageDispatcher()
    {
        return new Mock(UMOMessageDispatcher.class, "umoMessageDispatcher");
    }

    public static Mock getMockMessageDispatcherFactory()
    {
        return new Mock(UMOMessageDispatcherFactory.class, "umoMessageDispatcherFactory");
    }

    public static Mock getMockConnector()
    {
        return new Mock(UMOConnector.class, "umoConnector");
    }

    public static Mock getMockEvent()
    {
        return new Mock(UMOEvent.class, "umoEvent");
    }

    public static Mock getMockManager()
    {
        return new Mock(MuleManager.class, "muleManager");
    }

    public static Mock getMockEndpoint()
    {
        return new Mock(UMOEndpoint.class, "umoEndpoint");
    }

    public static Mock getMockEndpointURI()
    {
        return new Mock(UMOEndpointURI.class, "umoEndpointUri");
    }

    public static Mock getMockDescriptor()
    {
        return new Mock(UMODescriptor.class, "umoDescriptor");
    }

    public static Mock getMockTransaction()
    {
        return new Mock(UMOTransaction.class, "umoTransaction");
    }

    public static Mock getMockTransactionFactory()
    {
        return new Mock(UMOTransactionFactory.class, "umoTransactionFactory");
    }

}
