/*
 * $Id:XsltTransformerTestCase.java 5937 2007-04-09 22:35:04Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.impl.RequestContext;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOEvent;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.IOUtils;

import java.util.HashMap;
import java.util.Map;

public class XsltTransformerTestCase extends AbstractXmlTransformerTestCase
{

    private String srcData;
    private String resultData;

    // @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.getResourceAsString("cdcatalog.xml", getClass());
        resultData = IOUtils.getResourceAsString("cdcatalog.html", getClass());
    }

    public UMOTransformer getTransformer() throws Exception
    {
        XsltTransformer transformer = new XsltTransformer();
        transformer.setReturnClass(String.class);
        transformer.setXslFile("cdcatalog.xsl");
        transformer.setMaxActiveTransformers(42);
        transformer.initialise();
        return transformer;
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    // @Override
    public void testRoundtripTransform() throws Exception
    {
        // disable this test
    }

    public Object getTestData()
    {
        return srcData;
    }

    public Object getResultData()
    {
        return resultData;
    }

    public void testCustomTransformerFactoryClass() throws InitialisationException
    {
        XsltTransformer t = new XsltTransformer();
        t.setXslTransformerFactory("com.nosuchclass.TransformerFactory");
        t.setXslFile("cdcatalog.xsl");

        try
        {
            t.initialise();
            fail("should have failed with ClassNotFoundException");
        }
        catch (InitialisationException iex)
        {
            assertEquals(ClassNotFoundException.class, iex.getCause().getClass());
        }

        // try again with JDK default
        t.setXslTransformerFactory(null);
        t.initialise();
    }

    public void testTransformWithStaticParam() throws TransformerException, InitialisationException
    {

        String xml =
                "<node1>" +
                     "<subnode1>sub node 1 original value</subnode1>" +
                     "<subnode2>sub node 2 original value</subnode2>" +
                 "</node1>";

        String param = "sub node 2 cool new value";

        String expectedTransformedxml =
                "<node1>" +
                    "<subnode1>sub node 1 original value</subnode1>" +
                    "<subnode2>" + param + "</subnode2>" +
                "</node1>";

        String xsl =
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"" +
                     " xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\"" +
                     " xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">" +
                     "<xsl:param name=\"param1\"/>" +
                     "<xsl:template match=\"@*|node()\">" +
                         "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" +
                     "</xsl:template>" +
                     "<xsl:template match=\"@*|node()\">" +
                         "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" +
                     "</xsl:template>" +
                         "<xsl:template match=\"/node1/subnode2/text()\">" +
                         "<xsl:value-of select=\"$param1\"/>" +
                     "</xsl:template>" +
                 "</xsl:stylesheet>";

        XsltTransformer transformer = new XsltTransformer();

        transformer.setReturnClass(String.class);
        // set stylesheet
        transformer.setXslt(xsl);

        // set parameter
        Map params = new HashMap();
        params.put("param1", param);
        transformer.setTransformParameters(params);

        // init transformer
        transformer.initialise();

        // do transformation
        String transformerResult = (String) transformer.transform(xml);

        // remove doc type and CRLFs
        transformerResult = transformerResult.substring(transformerResult.indexOf("?>") + 2);

        assertTrue(transformerResult.indexOf(expectedTransformedxml) > -1);

    }

    public void testTransformWithDynamicParam() throws Exception
    {

        String xml =
                "<node1>" +
                     "<subnode1>sub node 1 original value</subnode1>" +
                     "<subnode2>sub node 2 original value</subnode2>" +
                 "</node1>";

        String param = "sub node 2 cool new value";

        String expectedTransformedxml =
                "<node1>" +
                    "<subnode1>sub node 1 original value</subnode1>" +
                    "<subnode2>" + param + "</subnode2>" +
                "</node1>";

        String xsl =
                "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\"" +
                    " xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\"" +
                    " xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">" +
                    "<xsl:param name=\"param1\"/>" +
                    "<xsl:template match=\"@*|node()\">" +
                        "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" +
                    "</xsl:template>" +
                    "<xsl:template match=\"@*|node()\">" +
                        "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" +
                    "</xsl:template>" +
                    "<xsl:template match=\"/node1/subnode2/text()\">" +
                        "<xsl:value-of select=\"$param1\"/>" +
                    "</xsl:template>" +
                "</xsl:stylesheet>";

        XsltTransformer transformer = new XsltTransformer();

        transformer.setReturnClass(String.class);
        
        // set stylesheet
        transformer.setXslt(xsl);

        // set parameter
        Map params = new HashMap();
        params.put("param1", "#getProperty(message,'myproperty')");
        transformer.setTransformParameters(params);

        // init transformer
        transformer.initialise();

        // set up UMOEventContext
        UMOEvent event = MuleTestUtils.getTestEvent("test message data", managementContext);
        event.getMessage().setProperty("myproperty", param);
        RequestContext.setEvent(event);

        // do transformation
        String transformerResult = (String) transformer.transform(xml);

        // remove doc type and CRLFs
        transformerResult = transformerResult.substring(transformerResult.indexOf("?>") + 2);

        assertTrue(transformerResult.indexOf(expectedTransformedxml) > -1);
    }

}
