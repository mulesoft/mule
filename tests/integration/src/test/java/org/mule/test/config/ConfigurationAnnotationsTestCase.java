/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.mule.api.AnnotatedObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.transformer.Transformer;
import org.mule.component.DefaultJavaComponent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.SystemUtils;

import javax.xml.namespace.QName;

import org.junit.Test;

/**
 * Test that configuration-based annotations are propagated to the appropriate runtime objects
 */
public class ConfigurationAnnotationsTestCase extends FunctionalTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                             "org/mule/config/spring/annotations.xml",
                             "org/mule/config/spring/annotations-config.xml"};
    }

    @Test
    public void testTransformerAnnotations()
    {
        Transformer stb = muleContext.getRegistry().lookupTransformer("StringtoByteArray");
        assertThat(stb, not(nullValue()));
        assertThat(getDocName(stb), is("stb-transformer"));
        assertThat(getDocDescription(stb), is("Convert a String to a Byte Array"));
        assertThat(getSourceFile(stb), is("annotations-config.xml"));
        assertThat(getSourceFileLine(stb), is(10));
        assertThat(getSourceElement(stb), is("<string-to-byte-array-transformer name=\"StringtoByteArray\" doc:name=\"stb-transformer\">" + SystemUtils.LINE_SEPARATOR +
                                             "<annotations>" + SystemUtils.LINE_SEPARATOR +
                                             "<doc:description>Convert a String to a Byte Array</doc:description>" + SystemUtils.LINE_SEPARATOR +
                                             "</annotations>" + SystemUtils.LINE_SEPARATOR +
                                             "</string-to-byte-array-transformer>"));
    }

    @Test
    public void testEndpointBuilderAnnotations()
    {
        EndpointBuilder in = muleContext.getRegistry().lookupEndpointBuilder("in");
        assertThat(in, not(nullValue()));
        assertThat(getDocName(in), is("inbound vm endpoint"));
        assertThat(getDocDescription(in), is("Accepts inbound messages"));
        assertThat(getSourceFile(in), is("annotations-config.xml"));
        assertThat(getSourceFileLine(in), is(16));
        assertThat(getSourceElement(in), is("<endpoint name=\"in\" address=\"vm://in\" exchange-pattern=\"request-response\" doc:name=\"inbound vm endpoint\">" + SystemUtils.LINE_SEPARATOR +
                                            "<annotations>" + SystemUtils.LINE_SEPARATOR +
                                            "<doc:description>Accepts inbound messages</doc:description>" + SystemUtils.LINE_SEPARATOR +
                                            "</annotations>" + SystemUtils.LINE_SEPARATOR +
                                            "</endpoint>"));
    }

    @Test
    public void testFlowAnnotations()
    {
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("Bridge");
        assertThat(flow, not(nullValue()));
        assertThat(getDocName(flow), is("Bridge flow"));
        assertThat(getDocDescription(flow), is("Main flow"));
        assertThat(getSourceFile(flow), is("annotations.xml"));
        assertThat(getSourceFileLine(flow), is(10));
        assertThat(getSourceElement(flow), is("<flow name=\"Bridge\" doc:name=\"Bridge flow\">" + SystemUtils.LINE_SEPARATOR +
                                              "<annotations>" + SystemUtils.LINE_SEPARATOR +
                                              "<doc:description>Main flow</doc:description>" + SystemUtils.LINE_SEPARATOR +
                                              "</annotations>" + SystemUtils.LINE_SEPARATOR +
                                              "<inbound-endpoint ref=\"in\" doc:name=\"inbound flow endpoint\">" + SystemUtils.LINE_SEPARATOR +
                                              "<transformer ref=\"StringtoByteArray\">" +
                                              "</transformer>" + SystemUtils.LINE_SEPARATOR +
                                              "</inbound-endpoint>" + SystemUtils.LINE_SEPARATOR +
                                              "<echo-component doc:name=\"echo\">" +
                                              "</echo-component>" + SystemUtils.LINE_SEPARATOR +
                                              "<outbound-endpoint ref=\"out\">" +
                                              "</outbound-endpoint>" + SystemUtils.LINE_SEPARATOR +
                                              "</flow>"));
    }

    @Test
    public void testFlowWithExceptionStrategyAnnotations()
    {
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("WithRefExceptionStrategy");
        assertThat(flow, not(nullValue()));
        assertThat(getDocName(flow), is("With Referenced Exception Strategy"));
        assertThat(getDocDescription(flow), is(nullValue()));
        assertThat(getSourceFile(flow), is("annotations.xml"));
        assertThat(getSourceFileLine(flow), is(28));
        assertThat(getSourceElement(flow), is("<flow name=\"WithRefExceptionStrategy\" doc:name=\"With Referenced Exception Strategy\">" +
                                              SystemUtils.LINE_SEPARATOR +
                                              "<echo-component doc:name=\"echo\">" +
                                              "</echo-component>" + SystemUtils.LINE_SEPARATOR +
                                              "<exception-strategy ref=\"Catch_Exception_Strategy\" doc:name=\"Reference Exception Strategy\">" +
                                              "</exception-strategy>" + SystemUtils.LINE_SEPARATOR +
                                              "</flow>"));
    }

    @Test
    public void testDefaultAnnotationsInNotAnnotatedObject()
    {
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("NotAnnotatedBridge");
        assertThat(flow, not(nullValue()));
        assertThat(getDocName(flow), is(nullValue()));
        assertThat(getDocDescription(flow), is(nullValue()));
        assertThat(getSourceFile(flow), is("annotations.xml"));
        assertThat(getSourceFileLine(flow), is(23));
        assertThat(getSourceElement(flow), is("<flow name=\"NotAnnotatedBridge\">" + SystemUtils.LINE_SEPARATOR +
                                              "<inbound-endpoint ref=\"notAnnotatedIn\"></inbound-endpoint>" + SystemUtils.LINE_SEPARATOR +
                                              "<echo-component></echo-component>" + SystemUtils.LINE_SEPARATOR +
                                              "</flow>"));
    }

    @Test
    public void testJavaComponentAnnotations()
    {
        DefaultJavaComponent echo = muleContext.getRegistry().lookupByType(DefaultJavaComponent.class).values().iterator().next();
        assertEquals("echo", getDocName(echo));
        assertThat(getSourceFile(echo), is("annotations.xml"));
        assertThat(getSourceFileLine(echo), is(17));
        assertThat(getSourceElement(echo), is("<echo-component doc:name=\"echo\">" +
                                              "</echo-component>"));
    }

    @Test
    public void testInboundEndpointAnnotations()
    {
        FlowConstruct flow = muleContext.getRegistry().lookupFlowConstruct("Bridge");
        ImmutableEndpoint ep = (ImmutableEndpoint) ((Flow)flow).getMessageSource();
        assertThat(ep, not(nullValue()));
        assertThat(getDocName(ep), is("inbound flow endpoint"));
        assertThat("Accepts inbound messages", getDocDescription(ep), nullValue());
        assertThat(getSourceFile(ep), is("annotations.xml"));
        assertThat(getSourceFileLine(ep), is(14));
        assertThat(getSourceElement(ep), is("<inbound-endpoint ref=\"in\" doc:name=\"inbound flow endpoint\">" + SystemUtils.LINE_SEPARATOR +
                                            "<transformer ref=\"StringtoByteArray\">" +
                                            "</transformer>" + SystemUtils.LINE_SEPARATOR +
                                            "</inbound-endpoint>"));
    }

    @Test
    public void testOutboundEndpointAnnotations()
    {
        OutboundEndpoint out = muleContext.getRegistry().lookupByType(OutboundEndpoint.class).values().iterator().next();
        assertThat(out, not(nullValue()));
        assertThat(getDocName(out), is(nullValue()));
        assertThat(getDocDescription(out), is(nullValue()));
        assertThat(getSourceFile(out), is("annotations.xml"));
        assertThat(getSourceFileLine(out), is(18));
        assertThat(getSourceElement(out), is("<outbound-endpoint ref=\"out\"></outbound-endpoint>"));
    }

    @Test
    public void testInsideSpringBeansAnnotations()
    {
        Transformer stb = muleContext.getRegistry().lookupTransformer("ManziTransformer");
        assertThat(stb, not(nullValue()));
        assertThat(getDocName(stb), is("manzi-transformer"));
        assertThat(getSourceFile(stb), is("annotations-config.xml"));
        assertThat(getSourceFileLine(stb), is(30));
        assertThat(getSourceElement(stb), is("<append-string-transformer message=\"Manzi\" name=\"ManziTransformer\" doc:name=\"manzi-transformer\"></append-string-transformer>"));
    }

    protected String getDocName(Object obj)
    {
        return (String) ((AnnotatedObject) obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "name"));
    }

    protected String getDocDescription(Object obj)
    {
        return (String) ((AnnotatedObject) obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "description"));
    }

    protected String getSourceFile(Object obj)
    {
        return (String) ((AnnotatedObject) obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName"));
    }

    protected Integer getSourceFileLine(Object obj)
    {
        return (Integer) ((AnnotatedObject) obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine"));
    }

    protected String getSourceElement(Object obj)
    {
        return (String) ((AnnotatedObject) obj).getAnnotation(new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceElement"));
    }
}
