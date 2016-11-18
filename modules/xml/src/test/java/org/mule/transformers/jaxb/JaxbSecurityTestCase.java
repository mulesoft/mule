/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.jaxb;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.mule.transformer.types.MimeTypes.APPLICATION_XML;
import static org.mule.util.xmlsecurity.XMLSecureFactories.EXPAND_ENTITIES_PROPERTY;
import static org.mule.util.xmlsecurity.XMLSecureFactories.EXTERNAL_ENTITIES_PROPERTY;
import org.mule.api.MuleEvent;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.construct.Flow;
import org.mule.module.xml.transformer.jaxb.JAXBUnmarshallerTransformer;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import org.junit.Rule;
import org.junit.Test;

/**
 * Test for XML XXE and Billion Laughs attacks
 */
public class JaxbSecurityTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    protected final String xmlWithEntities =
            "<?xml version=\"1.0\"?>\n" +
            "<!DOCTYPE order [\n" +
            "<!ELEMENT foo ANY >\n" +
            "<!ENTITY xxe SYSTEM 'file:%s' >\n" +
            "<!ENTITY lol \"0101\" >\n" +
            "]>\n" +
            "<Foo><bar>&xxe; &lol;</bar></Foo>";

    @Override
    protected String getConfigFile()
    {
        return "jaxb-transformer-security.xml";
    }

    protected String getXmlWithEntities()
    {
        return format(xmlWithEntities, IOUtils.getResourceAsUrl("xxe-passwd.txt", this.getClass()).getPath());
    }

    protected MuleEvent createEvent() throws Exception
    {
        MuleEvent testEvent = getTestEvent(TEST_MESSAGE);
        testEvent.getMessage().getDataType().setMimeType(APPLICATION_XML);
        testEvent.getMessage().setPayload(getXmlWithEntities());
        return testEvent;
    }

    @Test
    public void externalEntitiesEnabled() throws Exception
    {
        try {
            System.setProperty(EXTERNAL_ENTITIES_PROPERTY, "true");
            System.setProperty(EXPAND_ENTITIES_PROPERTY, "true");

            final MuleEvent testEvent = createEvent();
            final MuleEvent response = ((Flow) getFlowConstruct("testFlow")).process(testEvent);

            String payload = response.getMessage().getPayloadAsString();

            assertThat(payload, containsString("secret"));
            assertThat(payload, containsString("0101"));
        }
        finally
        {
            System.clearProperty(EXTERNAL_ENTITIES_PROPERTY);
            System.clearProperty(EXPAND_ENTITIES_PROPERTY);
        }
    }

    @Test
    public void expandsEntitiesEnabled() throws Exception
    {
        try {
            System.setProperty(EXPAND_ENTITIES_PROPERTY, "true");

            final MuleEvent testEvent = createEvent();
            final MuleEvent response = ((Flow) getFlowConstruct("testFlow")).process(testEvent);

            String payload = response.getMessage().getPayloadAsString();

            assertThat(payload, not(containsString("secret")));
            assertThat(payload, containsString("0101"));
        }
        finally
        {
            System.clearProperty(EXPAND_ENTITIES_PROPERTY);
        }
    }

    @Test
    public void expandsEntitiesWhenDisabled() throws Exception
    {
        try
        {
            final MuleEvent testEvent = createEvent();
            final MuleEvent response = ((Flow) getFlowConstruct("testFlow")).process(testEvent);

            String payload = response.getMessage().getPayloadAsString();

            fail("Should've thrown exception");
        } catch (TransformerMessagingException e) {
            assertThat(e.getTransformer(), instanceOf(JAXBUnmarshallerTransformer.class));
        }
    }
}
