/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.MuleException;
import org.mule.RegistryContext;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ThreadingProfile;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.providers.AbstractConnector;
import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.routing.response.AbstractResponseRouter;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMOOutboundRouterCollection;
import org.mule.umo.routing.UMOResponseRouterCollection;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.properties.JXPathPropertyExtractor;
import org.mule.util.properties.PropertyExtractor;

import java.util.List;

/**
 * This is an extended version of the same test covered in
 * {@link org.mule.test.spring.SpringNamespaceConfigBuilderTestCase}.  Both are translations of an
 * earlier (1.X) test.
 *
 * I realise this seems rather messy, and I did consider merging the two, but they often test different
 * things, and we would have lost quite a few tests on merging.  So I am afraid we are left with two
 * rather rambling, parallel tests.  But these tests examing "corner cases" no other tests cover, so
 * are quite valuable...
 */
public class SpringNamespaceConfigBuilderV2TestCase extends AbstractConfigBuilderTestCase
{

    public SpringNamespaceConfigBuilderV2TestCase()
    {
        super(true);
    }

    public String getConfigResources()
    {
        return "org/mule/test/spring/config2/test-xml-mule2-config.xml," +
                "org/mule/test/spring/config2/test-xml-mule2-config-split.xml," +
                "org/mule/test/spring/config2/test-xml-mule2-config-split-properties.xml";
    }

    // @Override
    public ConfigurationBuilder getBuilder()
    {
        return new MuleXmlConfigurationBuilder();
    }

    public void testPropertyExtractorConfig() throws Exception
    {
        UMOComponent d = managementContext.getRegistry().lookupComponent("propertyExtractorTestComponent");
        assertNotNull(d);
        UMOOutboundRouterCollection router = d.getOutboundRouter();
        assertNotNull(router);
        List routers = router.getRouters();
        assertNotNull(routers);
        assertEquals(1, routers.size());
        AbstractOutboundRouter theRouter = (AbstractOutboundRouter)routers.get(0);
        PropertyExtractor pe = theRouter.getPropertyExtractor();
        assertNotNull(pe);
        // the one we put in the config
        assertTrue(pe instanceof JXPathPropertyExtractor);
    }

    public void testPropertyExtractorResponseRouterConfig() throws Exception
    {
        UMOComponent d = managementContext.getRegistry().lookupComponent("propertyExtractorResponseRouterTestComponent");
        assertNotNull(d);
        UMOResponseRouterCollection router = d.getResponseRouter();
        assertNotNull(router);
        List routers = router.getRouters();
        assertNotNull(routers);
        assertEquals(1, routers.size());
        AbstractResponseRouter theRouter = (AbstractResponseRouter)routers.get(0);
        PropertyExtractor pe = theRouter.getPropertyExtractor();
        assertNotNull(pe);
        // the one we put in the config
        assertTrue(pe instanceof JXPathPropertyExtractor);
    }

    public void testPropertyTypesConfig() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("testPropertiesComponent");
        assertNotNull(c);
        Object obj = c.getServiceFactory().getOrCreate();
        assertNotNull(obj);
        assertTrue(obj instanceof Apple);
        assertTrue(((Apple) obj).isBitten()); 
        assertTrue(((Apple) obj).isWashed()); 
    }

    // no equivalent in 2.x for these
//    public void testMapPropertyTypesConfig() throws Exception
//    {
//        UMODescriptor d = managementContext.getRegistry().lookupService("testPropertiesComponent");
//        assertNotNull(d);
//        Map props = (Map)d.getProperties().get("propertiesMap");
//        assertNotNull(props);
//        assertNotNull(props.get("factoryObject"));
//        assertTrue(props.get("factoryObject") instanceof Orange);
//        assertNotNull(props.get("containerObject"));
//        assertTrue(props.get("containerObject") instanceof Apple);
//        assertNull(props.get("doesNotExist"));
//        assertEquals(System.getProperty("os.version"), props.get("osVersion"));
//        assertEquals("defaultValue", props.get("notASystemProperty"));
//        assertEquals("test1", props.get("test1"));
//        assertEquals("test2", props.get("test2"));
//    }
//
//    public void testListPropertyTypesConfig() throws Exception
//    {
//        UMODescriptor d = managementContext.getRegistry().lookupService("testPropertiesComponent");
//        assertNotNull(d);
//        List props = (List)d.getProperties().get("propertiesList");
//        assertNotNull(props);
//        assertEquals(6, props.size());
//        assertNotNull(props.get(0));
//        assertTrue(props.get(0) instanceof Orange);
//        assertEquals(System.getProperty("os.version"), props.get(1));
//        assertEquals("defaultValue", props.get(2));
//        assertEquals("test1", props.get(3));
//        assertEquals("test2", props.get(4));
//
//        // Container properties are added last
//        assertNotNull(props.get(5));
//        assertTrue(props.get(5) instanceof Apple);
//    }

    public void testEndpointURIParamsConfig()
    {
        UMOComponent d = managementContext.getRegistry().lookupComponent("testPropertiesComponent");
        assertNotNull(d);
        final UMOInboundRouterCollection router = d.getInboundRouter();
        assertNotNull(router);
        final List endpoints = router.getEndpoints();
        assertNotNull(endpoints);
        assertFalse(endpoints.isEmpty());
        final UMOEndpoint inboundEndpoint = (UMOEndpoint) endpoints.get(0);
        assertNotNull(inboundEndpoint);
        final List transformers = inboundEndpoint.getTransformers();
        assertFalse(transformers.isEmpty());
        assertNotNull(transformers.get(0));
        final List responseTransformers = inboundEndpoint.getResponseTransformers();
        assertFalse(responseTransformers.isEmpty());
        assertNotNull(responseTransformers.get(0));
    }

    // @Override
    public void testTransformerConfig()
    {
        // first of all test generic transformer configuration
        super.testTransformerConfig();

        UMOTransformer t = managementContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);

        // This will only work with the MuleXml Builder other implementations
        // will have to set this proerty manually or mimic Mules behaviour
        assertEquals("this was set from the manager properties!",
            ((TestCompressionTransformer)t).getBeanProperty1());
        assertEquals(12, ((TestCompressionTransformer)t).getBeanProperty2());

        assertEquals(t.getReturnClass(), java.lang.String.class);

        t = managementContext.getRegistry().lookupTransformer("TestTransformer");
        assertNotNull(t);
        assertEquals(t.getReturnClass(), byte[].class);
    }

    public void testSystemPropertyOverride()
    {
        // MULE-2183
//        assertEquals("default", managementContext.getRegistry().lookupObject("system-prop2"));
    }

// no longer overrride - made both configs same (and agree with 1.x)
//    /**
//     * The MuleXmlConfiguration builder provides special support for overloading
//     * config elements for threadingProfiles, queueProfiles and poolingProfiles, so
//     * that defaults can be declared in the main configuration but overiding elements
//     * can just replace certain values
//     *
//     * @throws MuleException
//     */
//    // @Override
//    public void testThreadingConfig() throws MuleException
//    {
//        // test config
//        ThreadingProfile tp = RegistryContext.getConfiguration().getDefaultThreadingProfile();
//        assertEquals(0, tp.getMaxBufferSize());
//        assertEquals(8, tp.getMaxThreadsActive());
//        assertEquals(4, tp.getMaxThreadsIdle());
//        assertEquals(0, tp.getPoolExhaustedAction());
//        assertEquals(60001, tp.getThreadTTL());
//
//        // test defaults
//        tp = RegistryContext.getConfiguration().getDefaultComponentThreadingProfile();
//        assertEquals(0, tp.getMaxBufferSize());
//        assertEquals(8, tp.getMaxThreadsActive());
//        assertEquals(4, tp.getMaxThreadsIdle());
//        assertEquals(0, tp.getPoolExhaustedAction());
//        assertEquals(60001, tp.getThreadTTL());
//
//        // test thatvalues not set retain a default value
//        AbstractConnector c = (AbstractConnector)managementContext.getRegistry().lookupConnector("dummyConnector");
//        tp = c.getDispatcherThreadingProfile();
//        assertEquals(2, tp.getMaxBufferSize());
//        assertEquals(8, tp.getMaxThreadsActive());
//        assertEquals(4, tp.getMaxThreadsIdle());
//        assertEquals(0, tp.getPoolExhaustedAction());
//        assertEquals(60001, tp.getThreadTTL());
//
//        UMOComponent component = managementContext.getRegistry().lookupComponent("appleComponent2");
//        assertTrue("component must be SedaComponent to get threading profile", component instanceof SedaComponent);
//        tp = ((SedaComponent) component).getThreadingProfile();
//        assertEquals(6, tp.getMaxBufferSize());
//        assertEquals(12, tp.getMaxThreadsActive());
//        assertEquals(6, tp.getMaxThreadsIdle());
//        assertEquals(0, tp.getPoolExhaustedAction());
//        assertEquals(60001, tp.getThreadTTL());
//    }

    // MULE-2458 (now has separate test)
//    public void testGlobalEndpointOverrides()
//    {
//        UMOImmutableEndpoint ep = managementContext.getRegistry().lookupEndpoint("orangeEndpoint");
//        assertNotNull(ep);
//        assertEquals(1, ep.getProperties().size());
//        assertEquals("value1", ep.getProperties().get("testGlobal"));
//        assertNull(ep.getFilter());
//
//        MuleDescriptor descriptor = (MuleDescriptor)managementContext.getRegistry().lookupService(
//            "orangeComponent");
//        assertNotNull(descriptor);
//        ep = descriptor.getInboundRouter().getEndpoint("orangeEndpoint");
//        assertNotNull(ep);
//        assertEquals("value1", ep.getProperties().get("testLocal"));
//        assertEquals("value1", ep.getProperties().get("testGlobal"));
//        assertEquals(2, ep.getProperties().size());
//        assertNotNull(ep.getFilter());
//    }
}
