/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */

package org.mule.module.jca;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.model.seda.SedaModel;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MuleResourceAdapterTestCase extends AbstractMuleContextTestCase
{
    private MuleResourceAdapter resourceAdapter;

    @Override
    protected void doSetUp() throws Exception
    {
        resourceAdapter = new MuleResourceAdapter();
        resourceAdapter.muleContext = muleContext;
        resourceAdapter.bootstrapContext = new MockBoostrapContext();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        resourceAdapter = null;
    }

    @Test
    public void testResolveModelName() throws ResourceException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setModelName("activationSpecModelName");
        resourceAdapter.setModelName("resourceAdaptorModelName");
        String modelName = resourceAdapter.resolveModelName(activationSpec);
        assertEquals("activationSpecModelName", modelName);
    }

    @Test
    public void testResolveModelNameFromResourceAdaptor() throws ResourceException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        resourceAdapter.setModelName("resourceAdaptorModelName");
        String modelName = resourceAdapter.resolveModelName(activationSpec);
        assertEquals("resourceAdaptorModelName", modelName);
    }

    @Test
    public void testResolveModelNameFromActivationSpec() throws ResourceException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setModelName("activationSpecModelName");
        String modelName = resourceAdapter.resolveModelName(activationSpec);
        assertEquals("activationSpecModelName", modelName);
    }

    @Test
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
            // expected
        }
    }

    @Test
    public void testGetJcaModelCreateNew() throws MuleException, ResourceException
    {
        JcaModel jcaModel = resourceAdapter.getJcaModel("jca");
        assertEquals("jca", jcaModel.getName());
    }

    @Test
    public void testGetJcaModelUseExisting() throws MuleException, ResourceException
    {
        Model jcaModel = new JcaModel();
        jcaModel.setName("jca");
        muleContext.getRegistry().registerModel(jcaModel);
        JcaModel jcaModel2 = resourceAdapter.getJcaModel("jca");
        assertEquals("jca", jcaModel2.getName());
        assertEquals(jcaModel, jcaModel2);
    }

    @Test
    public void testGetJcaModel3ExistingWrongType() throws MuleException
    {
        Model sedaModel = new SedaModel();
        sedaModel.setName("jca");
        muleContext.getRegistry().registerModel(sedaModel);
        try
        {
            resourceAdapter.getJcaModel("jca");
            fail("Exception Expected: Model is not JcaModel");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testCreateMessageInflowEndpoint() throws MuleException
    {
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setEndpoint("test://testEndpoint");
        ImmutableEndpoint endpoint = resourceAdapter.createMessageInflowEndpoint(activationSpec);
        assertEndpointAttributes(endpoint);
    }

    @Test
    public void testCreateJcaComponent() throws Exception
    {
        MessageEndpointFactory endpointFactory = new MockEndpointFactory();
        JcaModel jcaModel = resourceAdapter.getJcaModel("jca");
        MuleActivationSpec activationSpec = new MuleActivationSpec();
        activationSpec.setEndpoint("test://testEndpoint");
        InboundEndpoint endpoint = resourceAdapter.createMessageInflowEndpoint(activationSpec);

        Service service = resourceAdapter.createJcaService(endpointFactory, jcaModel, endpoint);

        // Check service
        assertNotNull(service);
        assertEquals("JcaService#" + endpointFactory.hashCode(), service.getName());
        assertNotNull(service);
        assertTrue(service instanceof JcaService);
        assertNotNull(service.getComponent());
        assertTrue(service.getComponent() instanceof JcaComponent);
        assertNotNull(((JcaComponent) service.getComponent()).workManager);
        testJcaService(service);

        testEndpoint(service);

        // Check endpoint
        ImmutableEndpoint endpoint2 = ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        assertEquals(endpoint, endpoint2);

        // Check service implementation
        assertEquals(endpointFactory, ((JcaComponent) service.getComponent()).messageEndpointFactory);
    }

    @Test
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

        MuleEndpointKey key = new MuleEndpointKey(endpointFactory, activationSpec);
        Service service = resourceAdapter.endpoints.get(key);

        assertEquals("JcaService#" + endpointFactory.hashCode(), service.getName());
        testJcaService(service);
        testEndpoint(service);
        
        assertTrue(service.getComponent() instanceof JcaComponent);
        
        assertEquals(endpointFactory, ((JcaComponent) service.getComponent()).messageEndpointFactory);

        // Additional activation with same endpointFactory does not increase size of
        // endpoint cache.
        resourceAdapter.endpointActivation(endpointFactory, activationSpec);
        assertEquals(1, resourceAdapter.endpoints.size());

        // Additional activation with new DefaultEndpointFactory instance increments size of
        // endpoint cahce
        resourceAdapter.endpointActivation(new MockEndpointFactory(), activationSpec);
        assertEquals(2, resourceAdapter.endpoints.size());

    }

    @Test
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
            // expected
        }
        assertEquals(0, resourceAdapter.endpoints.size());
    }

    @Test
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

    protected void testEndpoint(Service service)
    {
        ServiceCompositeMessageSource inboundRouterCollection = (ServiceCompositeMessageSource) service.getMessageSource();
        ImmutableEndpoint endpoint = inboundRouterCollection.getEndpoints().get(0);
        assertEndpointAttributes(endpoint);
    }

    protected void assertEndpointAttributes(ImmutableEndpoint endpoint)
    {
        assertNotNull(endpoint);
        assertNotNull(endpoint.getConnector());
        assertEquals("testEndpoint", endpoint.getEndpointURI().getAddress());
        assertEquals(MessageExchangePattern.ONE_WAY, endpoint.getExchangePattern());
    }

    protected void testJcaService(Service service)
    {
        // Check for a single inbound router, no outbound routers and no nested
        // routers
        assertNotNull(service);

        assertNotNull(service.getMessageSource());

        // InboundPassThroughRouter is now set in runtime rather than in service creation as in 1.4.x
        // assertEquals(1, service.getInboundRouter().getRouters().size());
        assertEquals(0, ((ServiceCompositeMessageSource) service.getMessageSource()).getMessageProcessors().size());
    }

}

class MockEndpointFactory implements MessageEndpointFactory
{

    public MessageEndpoint createEndpoint(XAResource arg0) throws UnavailableException
    {
        return null;
    }

    public boolean isDeliveryTransacted(Method arg0) throws NoSuchMethodException
    {
        return false;
    }
}

class MockActivationSpec implements ActivationSpec
{

    public void validate() throws InvalidPropertyException
    {
        // do nothing
    }

    public ResourceAdapter getResourceAdapter()
    {
        return null;
    }

    public void setResourceAdapter(ResourceAdapter arg0) throws ResourceException
    {
        // do nothing
    }
}

class MockBoostrapContext implements BootstrapContext
{

    public Timer createTimer() throws UnavailableException
    {
        return null;
    }

    public WorkManager getWorkManager()
    {
        return new TestJCAWorkManager();
    }

    public XATerminator getXATerminator()
    {
        return null;
    }
}
