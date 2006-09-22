/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.JXPathPropertyExtractor;
import org.mule.config.PoolingProfile;
import org.mule.config.PropertyExtractor;
import org.mule.config.ThreadingProfile;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.pool.CommonsPoolFactory;
import org.mule.impl.MuleDescriptor;
import org.mule.providers.AbstractConnector;
import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.routing.response.AbstractResponseRouter;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.umo.UMODescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOOutboundMessageRouter;
import org.mule.umo.routing.UMOResponseMessageRouter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ObjectPool;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleXmlConfigBuilderTestCase extends AbstractConfigBuilderTestCase
{
    public String getConfigResources()
    {
        return "test-xml-mule-config.xml,test-xml-mule-config-split.xml,test-xml-mule-config-split-properties.xml";
    }

    public ConfigurationBuilder getBuilder()
    {
        try {
            return new MuleXmlConfigurationBuilder();
        } catch (ConfigurationException e) {
            fail(e.getMessage());
            return null;
        }
    }

    public void testPropertyExtractorConfig() throws Exception {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("propertyExtractorTestComponent");
        assertNotNull(d);
        UMOOutboundMessageRouter router = d.getOutboundRouter();
        assertNotNull(router);
        List routers = router.getRouters();
        assertNotNull(routers);
        assertEquals(1, routers.size());
        AbstractOutboundRouter theRouter = (AbstractOutboundRouter) routers.get(0);
        PropertyExtractor pe = theRouter.getPropertyExtractor();
        assertNotNull(pe);
        // the one we put in the config
        assertTrue(pe instanceof JXPathPropertyExtractor);
    }

    public void testPropertyExtractorResponseRouterConfig() throws Exception {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("propertyExtractorResponseRouterTestComponent");
        assertNotNull(d);
        UMOResponseMessageRouter router = d.getResponseRouter();
        assertNotNull(router);
        List routers = router.getRouters();
        assertNotNull(routers);
        assertEquals(1, routers.size());
        AbstractResponseRouter theRouter = (AbstractResponseRouter) routers.get(0);
        PropertyExtractor pe = theRouter.getCorrelationExtractor();
        assertNotNull(pe);
        // the one we put in the config
        assertTrue(pe instanceof JXPathPropertyExtractor);
    }


    public void testPropertyTypesConfig() throws Exception
    {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("testPropertiesComponent");
        assertNotNull(d);
        assertNotNull(d.getProperties().get("factoryObject"));
        assertTrue(d.getProperties().get("factoryObject") instanceof Orange);
        assertNotNull(d.getProperties().get("containerObject"));
        assertTrue(d.getProperties().get("containerObject") instanceof Apple);
        assertNull(d.getProperties().get("doesNotExist"));
        assertEquals(System.getProperty("os.version"), d.getProperties().get("osVersion"));
        assertEquals("defaultValue", d.getProperties().get("notASystemProperty"));
        assertEquals("test1", d.getProperties().get("test1"));
        assertEquals("test2", d.getProperties().get("test2"));
    }

    public void testMapPropertyTypesConfig() throws Exception
    {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("testPropertiesComponent");
        assertNotNull(d);
        Map props = (Map) d.getProperties().get("propertiesMap");
        assertNotNull(props);
        assertNotNull(props.get("factoryObject"));
        assertTrue(props.get("factoryObject") instanceof Orange);
        assertNotNull(props.get("containerObject"));
        assertTrue(props.get("containerObject") instanceof Apple);
        assertNull(props.get("doesNotExist"));
        assertEquals(System.getProperty("os.version"), props.get("osVersion"));
        assertEquals("defaultValue", props.get("notASystemProperty"));
        assertEquals("test1", props.get("test1"));
        assertEquals("test2", props.get("test2"));
    }

    public void testListPropertyTypesConfig() throws Exception
    {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("testPropertiesComponent");
        assertNotNull(d);
        List props = (List) d.getProperties().get("propertiesList");
        assertNotNull(props);
        assertEquals(6, props.size());
        assertNotNull(props.get(0));
        assertTrue(props.get(0) instanceof Orange);
        assertEquals(System.getProperty("os.version"), props.get(1));
        assertEquals("defaultValue", props.get(2));
        assertEquals("test1", props.get(3));
        assertEquals("test2", props.get(4));

        // Container properties are added last
        assertNotNull(props.get(5));
        assertTrue(props.get(5) instanceof Apple);
    }

    public void testEndpointURIParamsConfig()
    {
        UMODescriptor d = MuleManager.getInstance().getModel().getDescriptor("testPropertiesComponent");
        assertNotNull(d);
        assertNotNull(d.getInboundEndpoint());
        assertNotNull(d.getInboundTransformer());
        assertNotNull(d.getInboundEndpoint().getResponseTransformer());
    }

    public void testTransformerConfig()
    {
        UMOTransformer t = MuleManager.getInstance().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);
        assertTrue(((TestCompressionTransformer) t).isSourceTypeSupported(String.class, true));

        // This will only work with the MuleXml Builder other implementations
        // will have to set this proerty manually or mimic Mules behaviour
        Assert.assertEquals("this was set from the manager properties!",
                            ((TestCompressionTransformer) t).getBeanProperty1());
        Assert.assertEquals(12, ((TestCompressionTransformer) t).getBeanProperty2());

        assertEquals(t.getReturnClass(), java.lang.String.class);

        t = MuleManager.getInstance().lookupTransformer("TestTransformer");
        assertNotNull(t);
        assertEquals(t.getReturnClass(), byte[].class);
    }

    public void testSystemPropertyOverride()
    {
        Map props = MuleManager.getInstance().getProperties();
        assertNotNull(props);
        assertEquals("default", props.get("system-prop2"));
    }

    /**
     * The MuleXmlConfiguration builder provides special support for overloading
     * config elements for threadingProfiles, queueProfiles and poolingProfiles,
     * so that defaults can be declared in the main configuration but overiding
     * elements can just replace certain values
     * 
     * @throws MuleException
     */
    public void testThreadingConfig() throws MuleException
    {
        // test config
        ThreadingProfile tp = MuleManager.getConfiguration().getDefaultThreadingProfile();
        assertEquals(0, tp.getMaxBufferSize());
        assertEquals(8, tp.getMaxThreadsActive());
        assertEquals(4, tp.getMaxThreadsIdle());
        assertEquals(0, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());

        // test defaults
        tp = MuleManager.getConfiguration().getComponentThreadingProfile();
        assertEquals(0, tp.getMaxBufferSize());
        assertEquals(8, tp.getMaxThreadsActive());
        assertEquals(4, tp.getMaxThreadsIdle());
        assertEquals(0, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());

        // test thatvalues not set retain a default value
        AbstractConnector c = (AbstractConnector) MuleManager.getInstance().lookupConnector("dummyConnector");
        tp = c.getDispatcherThreadingProfile();
        assertEquals(2, tp.getMaxBufferSize());
        assertEquals(8, tp.getMaxThreadsActive());
        assertEquals(4, tp.getMaxThreadsIdle());
        assertEquals(0, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());

        MuleDescriptor descriptor = (MuleDescriptor) MuleManager.getInstance()
                                                                .getModel()
                                                                .getDescriptor("appleComponent2");
        tp = descriptor.getThreadingProfile();
        assertEquals(6, tp.getMaxBufferSize());
        assertEquals(12, tp.getMaxThreadsActive());
        assertEquals(6, tp.getMaxThreadsIdle());
        assertEquals(0, tp.getPoolExhaustedAction());
        assertEquals(60001, tp.getThreadTTL());
    }

    public void testPoolingConfig()
    {
        // test config
        PoolingProfile pp = MuleManager.getConfiguration().getPoolingProfile();
        assertEquals(8, pp.getMaxActive());
        assertEquals(4, pp.getMaxIdle());
        assertEquals(4000, pp.getMaxWait());
        assertEquals(ObjectPool.DEFAULT_EXHAUSTED_ACTION, pp.getExhaustedAction());
        assertEquals(1, pp.getInitialisationPolicy());
        assertTrue(pp.getPoolFactory() instanceof CommonsPoolFactory);

        // test override
        MuleDescriptor descriptor = (MuleDescriptor) MuleManager.getInstance()
                                                                .getModel()
                                                                .getDescriptor("appleComponent2");
        pp = descriptor.getPoolingProfile();

        assertEquals(8, pp.getMaxActive());
        assertEquals(4, pp.getMaxIdle());
        assertEquals(4000, pp.getMaxWait());
        assertEquals(ObjectPool.DEFAULT_EXHAUSTED_ACTION, pp.getExhaustedAction());
        assertEquals(2, pp.getInitialisationPolicy());
    }

    public void testGlobalEndpointOverrides()
    {
        UMOEndpoint ep = MuleManager.getInstance().lookupEndpoint("orangeEndpoint");
        assertNotNull(ep);
        assertEquals(1, ep.getProperties().size());
        assertEquals("value1", ep.getProperties().get("testGlobal"));
        assertNull(ep.getFilter());

        MuleDescriptor descriptor = (MuleDescriptor) MuleManager.getInstance()
                                                                .getModel()
                                                                .getDescriptor("orangeComponent");
        assertNotNull(descriptor);
        ep = descriptor.getInboundRouter().getEndpoint("orangeEndpoint");
        assertNotNull(ep);
        assertEquals(2, ep.getProperties().size());
        assertEquals("value1", ep.getProperties().get("testGlobal"));
        assertEquals("value1", ep.getProperties().get("testLocal"));
        assertNotNull(ep.getFilter());
    }
}
