/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */


package org.mule.ra;

import org.mule.impl.model.seda.SedaModel;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.model.UMOModel;
import org.mule.umo.routing.UMOInboundRouterCollection;

import java.lang.reflect.Method;
import java.util.Timer;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.xa.XAResource;

public class MuleResourceAdapterTestCase extends AbstractMuleTestCase
{
    private MuleResourceAdapter resourceAdapter;

    protected void doSetUp() throws Exception
    {
        resourceAdapter = new MuleResourceAdapter();
        resourceAdapter.managementContext = managementContext;
        resourceAdapter.bootstrapContext = new MockBoostrapContext();
    }

    protected void doTearDown() throws Exception
    {
        resourceAdapter = null;
    }

    public void testResolveModelName() throws ResourceException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setModelName("activationSpecModelName");
        resourceAdapter.setModelName("resourceAdaptorModelName");
        String modelName = resourceAdapter.resolveModelName(activationSpec);
        assertEquals("activationSpecModelName", modelName);
    }

    public void testResolveModelNameFromResourceAdaptor() throws ResourceException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        resourceAdapter.setModelName("resourceAdaptorModelName");
        String modelName = resourceAdapter.resolveModelName(activationSpec);
        assertEquals("resourceAdaptorModelName", modelName);
    }

    public void testResolveModelNameFromActivationSpec() throws ResourceException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setModelName("activationSpecModelName");
        String modelName = resourceAdapter.resolveModelName(activationSpec);
        assertEquals("activationSpecModelName", modelName);
    }

    public void testResolveModelModelNameMissing()
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        try
        {
            resourceAdapter.resolveModelName(activationSpec);
            fail("Exception expected: No modelName set.");
        }
        catch (ResourceException e)
        {
        }
    }

    public void testGetJcaModelCreateNew() throws UMOException, ResourceException
    {
        JcaModel jcaModel = resourceAdapter.getJcaModel("jca");
        assertEquals("jca", jcaModel.getName());
    }

    public void testGetJcaModelUseExisting() throws UMOException, ResourceException
    {
        UMOModel jcaModel = new JcaModel();
        jcaModel.setName("jca");
        managementContext.getRegistry().registerModel(jcaModel);
        JcaModel jcaModel2 = resourceAdapter.getJcaModel("jca");
        assertEquals("jca", jcaModel2.getName());
        assertEquals(jcaModel, jcaModel2);
    }

    public void testGetJcaModel3ExistingWrongType() throws UMOException
    {
        UMOModel sedaModel = new SedaModel();
        sedaModel.setName("jca");
        managementContext.getRegistry().registerModel(sedaModel);
        try
        {
            resourceAdapter.getJcaModel("jca");
            fail("Exception Expected: Model is not JcaModel");
        }
        catch (Exception e)
        {
        }
    }

    public void testCreateMessageInflowEndpoint() throws UMOException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setEndpoint("test://testEndpoint");
        UMOImmutableEndpoint endpoint = resourceAdapter.createMessageInflowEndpoint(activationSpec);
        testEndpoint(endpoint);
    }

    public void testCreateJcaComponent() throws Exception
    {
        MessageEndpointFactory endpointFactory = new MockEndpointFactory();
        JcaModel jcaModel = resourceAdapter.getJcaModel("jca");
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setEndpoint("test://testEndpoint");
        UMOImmutableEndpoint endpoint = resourceAdapter.createMessageInflowEndpoint(activationSpec);

        UMOComponent component = resourceAdapter.createJcaComponent(endpointFactory, jcaModel, endpoint);

        // Check component
        assertNotNull(component);
        assertEquals("JcaComponent#" + endpointFactory.hashCode(), component.getName());
        assertNotNull(component);
        assertTrue(component instanceof JcaComponent);
        assertNotNull(((JcaComponent) component).workManager);
        testJcaComponent(component);

        testEndpoint(component);

        // Check endpoint
        UMOEndpoint endpoint2 = (UMOEndpoint) component.getInboundRouter().getEndpoints().get(0);
        assertEquals(endpoint, endpoint2);

        // Check component implementation
        assertEquals(endpointFactory, component.getServiceFactory().getOrCreate());
    }

    public void testendpointActivationOK() throws Exception
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setResourceAdapter(resourceAdapter);
        activationSpec.setModelName("jcaModel");
        activationSpec.setEndpoint("test://testEndpoint");
        MessageEndpointFactory endpointFactory = new MockEndpointFactory();

        assertEquals(0, resourceAdapter.endpoints.size());

        resourceAdapter.endpointActivation(endpointFactory, activationSpec);
        assertEquals(1, resourceAdapter.endpoints.size());

        MuleEndpointKey key = new MuleEndpointKey(endpointFactory, (MuleActivationSpec) activationSpec);
        UMOComponent component = (UMOComponent) resourceAdapter.endpoints.get(key);

        assertEquals("JcaComponent#" + endpointFactory.hashCode(), component.getName());
        testJcaComponent(component);
        testEndpoint(component);
        assertEquals(endpointFactory, component.getServiceFactory().getOrCreate());

        // Additional activation with same endpointFactory does not increase size of
        // endpoint cache.
        resourceAdapter.endpointActivation(endpointFactory, activationSpec);
        assertEquals(1, resourceAdapter.endpoints.size());

        // Additional activation with new EndpointFactory instance increments size of
        // endpoint cahce
        resourceAdapter.endpointActivation(new MockEndpointFactory(), activationSpec);
        assertEquals(2, resourceAdapter.endpoints.size());

    }

    public void testendpointActivationIncorrectActivationSpec()
    {
        try
        {
            ActivationSpec activationSpec = new MockActivationSpec();
            activationSpec.setResourceAdapter(resourceAdapter);
            resourceAdapter.endpointActivation(new MockEndpointFactory(), activationSpec);
            fail("Exception expected: Invalid ActivationSpec type");
        }
        catch (ResourceException e)
        {
        }
        assertEquals(0, resourceAdapter.endpoints.size());
    }

    public void testendpointDeactivationOK() throws ResourceException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setResourceAdapter(resourceAdapter);
        activationSpec.setModelName("jcaModel");
        activationSpec.setEndpoint("test://test");
        MessageEndpointFactory endpointFactory = new MockEndpointFactory();
        assertEquals(0, resourceAdapter.endpoints.size());
        resourceAdapter.endpointActivation(endpointFactory, activationSpec);
        assertEquals(1, resourceAdapter.endpoints.size());
        resourceAdapter.endpointDeactivation(endpointFactory, activationSpec);
        assertEquals(0, resourceAdapter.endpoints.size());
    }

    protected void testEndpoint(UMOComponent component)
    {
        UMOInboundRouterCollection inboundRouterCollection = component.getInboundRouter();
        UMOEndpoint endpoint = (UMOEndpoint) inboundRouterCollection.getEndpoints().get(0);
        testEndpoint(endpoint);
    }

    protected void testEndpoint(UMOImmutableEndpoint endpoint)
    {
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertEquals("testEndpoint", endpoint.getEndpointURI().getAddress());
        assertEquals(false, endpoint.isSynchronous());
    }

    protected void testJcaComponent(UMOComponent component)
    {
        // Check for a single inbound router, no outbound routers and no nested
        // routers
        assertNotNull(component);

        assertNotNull(component.getInboundRouter());

        // InboundPassThroughRouter is now set in runtime rather than in component creation as in 1.4.x
        // assertEquals(1, component.getInboundRouter().getRouters().size());
        assertEquals(0, component.getInboundRouter().getRouters().size());
    }

}

class MockEndpointFactory implements MessageEndpointFactory
{

    public MessageEndpoint createEndpoint(XAResource arg0) throws UnavailableException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isDeliveryTransacted(Method arg0) throws NoSuchMethodException
    {
        // TODO Auto-generated method stub
        return false;
    }
}

class MockActivationSpec implements ActivationSpec
{

    public void validate() throws InvalidPropertyException
    {
        // TODO Auto-generated method stub

    }

    public ResourceAdapter getResourceAdapter()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException
    {
        // TODO Auto-generated method stub

    }
}

class MockBoostrapContext implements BootstrapContext
{

    public Timer createTimer() throws UnavailableException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public WorkManager getWorkManager()
    {
        return new TestJCAWorkManager();
    }

    public XATerminator getXATerminator()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
