/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.module.xml.util.XMLUtils.SAXON_TRANSFORMER_FACTORY;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.URIResolver;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.XsltTransformer;
import org.mule.module.xml.util.LocalURIResolver;
import org.mule.module.xml.util.XMLTestUtils;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformers.xml.AbstractXmlTransformerTestCase;
import org.mule.util.IOUtils;

import net.sf.saxon.Controller;
import net.sf.saxon.TransformerFactoryImpl;

public class XsltTransformerTestCase extends AbstractXmlTransformerTestCase
{

    public static final String VALID_XSL_FILENAME = "cdcatalog.xsl";

    private String srcData;
    private String resultData;

    @Override
    protected void doSetUp() throws Exception
    {
        srcData = IOUtils.getResourceAsString("cdcatalog.xml", getClass());
        resultData = IOUtils.getResourceAsString("cdcatalog.html", getClass());
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        XsltTransformer transformer = new XsltTransformer();
        transformer.setReturnDataType(DataTypeFactory.STRING);
        transformer.setXslFile(VALID_XSL_FILENAME);
        transformer.setMaxActiveTransformers(42);
        transformer.setMuleContext(muleContext);
        transformer.initialise();
        return transformer;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    @Override
    public void testRoundtripTransform() throws Exception
    {
        // disable this test
    }

    @Override
    public Object getTestData()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("ListTitle", "MyList");
        props.put("ListRating", new Integer(6));
        return new DefaultMuleMessage(srcData, props, muleContext);
    }

    @Override
    public Object getResultData()
    {
        return resultData;
    }

    @Test
    public void testAllXmlMessageTypes() throws Exception
    {
        List<?> list = XMLTestUtils.getXmlMessageVariants("cdcatalog.xml");
        Iterator<?> it = list.iterator();

        Object expectedResult = getResultData();
        assertThat(expectedResult, is(not(nullValue())));

        Object msg, result;
        while (it.hasNext())
        {
            msg = it.next();
            result = getTransformer().transform(msg);
            assertThat(result, is(not(nullValue())));
            assertThat(compareResults(expectedResult, result), equalTo(true));
        }
    }

    @Test
    public void testTransformXMLStreamReader() throws Exception
    {
        Object expectedResult = getResultData();
        assertThat(expectedResult, is(not(nullValue())));

        XsltTransformer transformer = (XsltTransformer) getTransformer();

        InputStream is = IOUtils.getResourceAsStream("cdcatalog.xml", XMLTestUtils.class);

        XMLStreamReader sr = XMLUtils.toXMLStreamReader(transformer.getXMLInputFactory(), is);

        Object result = transformer.transform(sr);
        assertThat(result, is(not(nullValue())));
        assertThat(compareResults(expectedResult, result), equalTo(true));
    }

    @Test
    public void testTransformInputStream() throws Exception
    {
        Object expectedResult = getResultData();
        assertThat(expectedResult, is(not(nullValue())));

        XsltTransformer transformer = (XsltTransformer) getTransformer();

        InputStream is = IOUtils.getResourceAsStream("cdcatalog.xml", XMLTestUtils.class);
        InputStream mockedInputStream = spy(is);

        Object result = transformer.transform(mockedInputStream);
        assertThat(result, is(not(nullValue())));
        assertThat(compareResults(expectedResult, result), is(true));
        verify(mockedInputStream).close();
    }

    @Test
    public void testCustomTransformerFactoryClass() throws InitialisationException
    {
        XsltTransformer t = new XsltTransformer();
        t.setXslFile(VALID_XSL_FILENAME);
        t.initialise();

        assertThat(t.getTransformerFactory().getClass().getName(), equalTo(SAXON_TRANSFORMER_FACTORY));

    }

    @Test
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

        String xsl = someXslText();

        XsltTransformer transformer = new XsltTransformer();

        transformer.setMuleContext(muleContext);
        transformer.setReturnDataType(DataTypeFactory.STRING);
        // set stylesheet
        transformer.setXslt(xsl);

        // set parameter
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param1", param);
        transformer.setContextProperties(params);

        // init transformer
        transformer.initialise();

        // do transformation
        String transformerResult = (String) transformer.transform(xml);

        // remove doc type and CRLFs
        transformerResult = transformerResult.substring(transformerResult.indexOf("?>") + 2);

        assertThat(transformerResult.indexOf(expectedTransformedxml), greaterThan(-1));

    }

    private String someXslText()
    {
        return "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\"" +
            " xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\"" +
            " xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">" +
            "<xsl:param name=\"param1\"/>" +
            "<xsl:template match=\"@*|node()\">" +
            "<xsl:copy><xsl:apply-templates select=\"@*|node()\"/></xsl:copy>" +
            "</xsl:template>" +
            "<xsl:template match=\"/node1/subnode2/text()\">" +
            "<xsl:value-of select=\"$param1\"/>" +
            "</xsl:template>" +
            "</xsl:stylesheet>";
    }

    private String someXslTextWithCData()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
            "   version=\"1.0\">\n" +
            "   \n" +
            "   <xsl:output cdata-section-elements=\"b\"/>\n" +
            "   \n" +
            "   <xsl:template match=\"@*|node()\">\n" +
            "      <xsl:copy>\n" +
            "         <xsl:apply-templates select=\"@*|node()\"/>\n" +
            "      </xsl:copy>\n" +
            "   </xsl:template>\n" +
            "</xsl:stylesheet>";
    }

    @Test
    public void testTransformWithCData() throws Exception
    {
        String expectedTransformedxml =
            "<a>\n"
                + "    <b><![CDATA[<c>hello</c>]]></b>\n"
                + "</a>";

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + expectedTransformedxml;

        String xsl = someXslTextWithCData();

        XsltTransformer transformer = new XsltTransformer();

        transformer.setMuleContext(muleContext);
        transformer.setReturnDataType(DataTypeFactory.STRING);
        transformer.setMuleContext(muleContext);
        // set stylesheet
        transformer.setXslt(xsl);

        // init transformer
        transformer.initialise();

        MuleMessage message = new DefaultMuleMessage(xml, muleContext);
        // do transformation
        String transformerResult = (String) transformer.transform(message);

        // remove doc type and CRLFs
        transformerResult = transformerResult.substring(transformerResult.indexOf("?>") + 2);

        assertThat(transformerResult.indexOf(expectedTransformedxml), greaterThan(-1));
    }

    @Test
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

        String xsl = someXslText();

        XsltTransformer transformer = new XsltTransformer();

        transformer.setMuleContext(muleContext);
        transformer.setReturnDataType(DataTypeFactory.STRING);
        transformer.setMuleContext(muleContext);
        // set stylesheet
        transformer.setXslt(xsl);

        // set parameter
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param1", "#[header:myproperty]");
        transformer.setContextProperties(params);

        // init transformer
        transformer.initialise();

        MuleMessage message = new DefaultMuleMessage(xml, muleContext);
        message.setOutboundProperty("myproperty", param);
        // do transformation
        String transformerResult = (String) transformer.transform(message);

        // remove doc type and CRLFs
        transformerResult = transformerResult.substring(transformerResult.indexOf("?>") + 2);

        assertThat(transformerResult.indexOf(expectedTransformedxml), greaterThan(-1));
    }

    @Test
    public void testInitialiseMustLoadXsltFile_dontLoadIfThereIsXslText() throws Exception
    {
        XsltTransformer xsltTransformer = new XsltTransformer();
        xsltTransformer.setXslt(someXslText());
        try
        {
            xsltTransformer.initialise();
            assertThat(xsltTransformer.getXslt(), equalTo(someXslText()));
        }
        catch (InitialisationException e)
        {
            fail("Should not have thrown an exception: " + e);
        }
    }

    @Test
    public void testInitialiseMustLoadXsltFile_ThrowExceptionIfNoXslTextNorFile() throws Exception
    {
        XsltTransformer xsltTransformer = new XsltTransformer();
        try
        {
            xsltTransformer.initialise();
            fail("Should have thrown an exception because nor xslt-text nor xslt-file was set.");
        }
        catch (InitialisationException e)
        {
            assertThat(e.getMessage().contains("xsl-file or xsl-text"), equalTo(true));
        }
    }

    @Test
    public void testInitialiseMustLoadXsltFile_ThrowExceptionIfXslFileDoesNotExist() throws Exception
    {
        XsltTransformer xsltTransformer = new XsltTransformer();
        String someNonExistentFileName = "some nonexistent file";
        xsltTransformer.setXslFile(someNonExistentFileName);
        try
        {
            xsltTransformer.initialise();
            fail("Should have thrown an exception because file '" + someNonExistentFileName
                     + "' does not exist.");
        }
        catch (InitialisationException e)
        {
            assertThat(e.getMessage().contains(someNonExistentFileName), equalTo(true));
        }
    }

    @Test
    public void testInitialiseMustLoadXsltFile_LoadsFromXslFile() throws Exception
    {
        XsltTransformer xsltTransformer = new XsltTransformer();
        xsltTransformer.setXslFile(VALID_XSL_FILENAME);
        try
        {
            xsltTransformer.initialise();
            assertThat(xsltTransformer.getXslt(), is(not(nullValue())));
            String someTextThatIsInTheXslFile = "My CD Collection";
            assertThat(xsltTransformer.getXslt(),
                    containsString(someTextThatIsInTheXslFile));
        }
        catch (InitialisationException e)
        {
            fail("Should NOT have thrown an exception because file '" + VALID_XSL_FILENAME
                     + "' DOES exist.");
        }
    }

    @Test
    public void testInitialiseUriResolverIfNotSet() throws Exception
    {
        XsltTransformer xsltTransformer = new XsltTransformer();
        xsltTransformer.setXslFile(VALID_XSL_FILENAME);

        xsltTransformer.initialise();
        assertThat(xsltTransformer.getUriResolver(), instanceOf(LocalURIResolver.class));
    }

    @Test
    public void testInitialiseMaintainsUriResolverIfSet() throws Exception
    {
        XsltTransformer xsltTransformer = new XsltTransformer();
        xsltTransformer.setXslFile(VALID_XSL_FILENAME);

        URIResolver uriResolver = new LocalURIResolver();
        xsltTransformer.setUriResolver(uriResolver);
        xsltTransformer.initialise();
        assertThat(uriResolver, equalTo(xsltTransformer.getUriResolver()));
    }

    @Test
    public void testTransformerUnderlyingControllerIsReset() throws Exception
    {
        performTransformation(true);

        Controller controller = ((net.sf.saxon.jaxp.TransformerImpl) TestTransformerFactoryImpl.TRANSFORMER).getUnderlyingController();

        assertThat(controller.getInitialContextItem(), is(nullValue()));
    }
    
    @Test
    public void testTransformerUnderlyingControllerIsNotResetByDefault() throws Exception
    {
        performTransformation(false);
        
        Controller controller = ((net.sf.saxon.jaxp.TransformerImpl) TestTransformerFactoryImpl.TRANSFORMER).getUnderlyingController();

        assertThat(controller.getInitialContextItem(), is(not(nullValue())));
    }

    private void performTransformation(boolean resetController) throws InitialisationException, TransformerException
    {
        String xml =
                "<node1>" +
                     "<subnode1>sub node 1 original value</subnode1>" +
                     "<subnode2>sub node 2 original value</subnode2>" +
                 "</node1>";

        String param = "sub node 2 cool new value";

        String xsl = someXslText();

        XsltTransformer transformer = new XsltTransformer();
        transformer.setXslTransformerFactory(TestTransformerFactoryImpl.class.getTypeName());
        transformer.setResetControllerAfterEachTransformation(resetController);

        transformer.setMuleContext(muleContext);
        transformer.setReturnDataType(DataTypeFactory.STRING);
        // set stylesheet
        transformer.setXslt(xsl);

        // set parameter
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("param1", param);
        transformer.setContextProperties(params);

        // init transformer
        transformer.initialise();

        transformer.transform(xml);
    }

}
