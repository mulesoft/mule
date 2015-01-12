/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.routing.ExpressionMessageInfoMapping;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractConfigBuilderTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.mule.TestCompressionTransformer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * This is an extended version of the same test covered in
 * {@link SpringNamespaceConfigBuilderTestCase}.  Both are translations of an
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
        setDisposeContextPerClass(true);
    }

    @Override
    public String[] getConfigFiles()
    {
        return new String[] {
            "org/mule/test/spring/config2/test-xml-mule2-config.xml",
            "org/mule/test/spring/config2/test-xml-mule2-config-split.xml",
            "org/mule/test/spring/config2/test-xml-mule2-config-split-properties.xml"
        };
    }

    @Test
    public void testMessageInfoMappingConfig() throws Exception
    {
        Service d = muleContext.getRegistry().lookupService("msgInfoMappingTestComponent");
        assertNotNull(d);

        final MessageInfoMapping mapping = d.getMessageInfoMapping();
        assertTrue(mapping instanceof ExpressionMessageInfoMapping);

        Map props = new HashMap();
        props.put("id", "myID123");
        props.put("correlation", "myCorrelationID456");
        MuleMessage msg = new DefaultMuleMessage("foo", props, muleContext);
        assertEquals("myID123",mapping.getMessageId(msg));
        assertEquals("myCorrelationID456",mapping.getCorrelationId(msg));
    }

    @Test
    public void testPropertyTypesConfig() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("testPropertiesComponent");
        assertNotNull(c);
        Object obj = getComponent(c);
        assertNotNull(obj);
        assertTrue(obj instanceof Apple);
        assertTrue(((Apple) obj).isBitten());
        assertTrue(((Apple) obj).isWashed());
    }

    @Test
    public void testEndpointURIParamsConfig()
    {
        Service d = muleContext.getRegistry().lookupService("testPropertiesComponent");
        assertNotNull(d);
        final ServiceCompositeMessageSource router = (ServiceCompositeMessageSource) d.getMessageSource();
        assertNotNull(router);
        final List endpoints = router.getEndpoints();
        assertNotNull(endpoints);
        assertFalse(endpoints.isEmpty());
        final ImmutableEndpoint inboundEndpoint = (ImmutableEndpoint) endpoints.get(0);
        assertNotNull(inboundEndpoint);
        final List transformers = inboundEndpoint.getTransformers();
        assertFalse(transformers.isEmpty());
        assertNotNull(transformers.get(0));
        final List responseTransformers = inboundEndpoint.getResponseTransformers();
        assertFalse(responseTransformers.isEmpty());
        assertNotNull(responseTransformers.get(0));
    }

    @Override
    public void testTransformerConfig()
    {
        // first of all test generic transformer configuration
        super.testTransformerConfig();

        Transformer t = muleContext.getRegistry().lookupTransformer("TestCompressionTransformer");
        assertNotNull(t);
        assertTrue(t instanceof TestCompressionTransformer);

        // This will only work with the MuleXml Builder other implementations
        // will have to set this proerty manually or mimic Mules behaviour
        assertEquals("this was set from the manager properties!",
            ((TestCompressionTransformer)t).getBeanProperty1());
        assertEquals(12, ((TestCompressionTransformer)t).getBeanProperty2());

        assertEquals(t.getReturnClass(), java.lang.String.class);

        t = muleContext.getRegistry().lookupTransformer("TestTransformer");
        assertNotNull(t);
        assertEquals(t.getReturnClass(), byte[].class);
    }
}
