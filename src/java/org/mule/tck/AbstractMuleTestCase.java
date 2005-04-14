/*
 * $Header:
 * /cvsroot/mule/mule/src/test/org/mule/test/mule/AbstractMuleTestCase.java,v
 * 1.7 2003/11/24 09:58:47 rossmason Exp $ $Revision$ $Date: 2003/11/24
 * 09:58:47 $
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.tck;

import com.mockobjects.dynamic.Mock;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.impl.*;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassHelper;

/**
 * <code>AbstractMuleTestCase</code> is a base class for Mule testcases.  This
 * implementation provides services to test code for creating mock and test objects.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMuleTestCase extends NamedTestCase
{
    public AbstractMuleTestCase()
    {
        super();
    }

    public static UMOManager getManager() throws Exception
    {
        UMOManager manager;
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        manager = MuleManager.getInstance();
        manager.setModel(new MuleModel());
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration().getPoolingProfile().setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_NO_COMPONENTS);
        return manager;
    }

    public static UMOEndpoint getTestEndpoint(String name, String type) throws Exception
    {
        UMOEndpoint endpoint = new MuleEndpoint();
        //need to build endpoint this way to avoid depenency to any endpoint jars
        UMOConnector connector = null;
        connector = (UMOConnector) ClassHelper.loadClass("org.mule.tck.testmodels.mule.TestConnector", AbstractMuleTestCase.class).newInstance();

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
        UMOEvent event = new MuleEvent(new MuleMessage(data, null), getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER), session, true);
        return event;
    }

    public static UMOTransformer getTestTransformer()
    {
        return new TestCompressionTransformer();
    }

    public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor) throws Exception
    {
        UMOComponent component = getTestComponent(descriptor);

        UMOSession session = getTestSession(component);
        UMOEvent event = new MuleEvent(new MuleMessage(data, null), getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER), getTestSession(component), true);
        return event;
    }

    public static UMOEvent getTestEvent(Object data, UMOEndpoint endpoint) throws Exception
    {
        UMOSession session = getTestSession(getTestComponent(getTestDescriptor("string", String.class.getName())));
        UMOEvent event = new MuleEvent(new MuleMessage(data, null), endpoint, session, true);
        return event;
    }

     public static UMOEvent getTestEvent(Object data, MuleDescriptor descriptor, UMOEndpoint endpoint) throws UMOException
    {
        UMOSession session = getTestSession(getTestComponent(descriptor));
        UMOEvent event = new MuleEvent(new MuleMessage(data, null), endpoint, session, true);
        return event;
    }

    public static UMOSession getTestSession(UMOComponent component)
    {
        return new MuleSession((MuleComponent) component, null);
    }

    public static TestConnector getTestConnector()
    {
        return new TestConnector();
    }

    public static MuleComponent getTestComponent(MuleDescriptor descriptor)
    {
        return new MuleComponent(descriptor);
    }

    public static MuleDescriptor getTestDescriptor(String name, String implementation) throws Exception
    {
        MuleDescriptor descriptor = new MuleDescriptor();
        descriptor.setExceptionListener(new DefaultExceptionStrategy());
        descriptor.setName(name);
        descriptor.setImplementation(implementation);
        descriptor.setOutboundEndpoint(getTestEndpoint("test1", UMOEndpoint.ENDPOINT_TYPE_SENDER));
        descriptor.initialise();
             
        descriptor.getPoolingProfile().setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_NO_COMPONENTS);

        return descriptor;
    }

    public static UMOManager getTestManager()
    {
        return MuleManager.getInstance();
    }

    public static Mock getMockSession()
    {
        return new Mock(UMOSession.class, "umoSession");
    }

    public static Mock getMockMessageDispatcher()
    {
        return new Mock(UMOMessageDispatcher.class, "umoConnectorSession");
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
