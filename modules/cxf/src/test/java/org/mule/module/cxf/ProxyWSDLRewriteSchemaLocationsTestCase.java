/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.cxf.helpers.DOMUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class ProxyWSDLRewriteSchemaLocationsTestCase extends FunctionalTestCase
{
    @Rule
    public final DynamicPort httpPortProxy = new DynamicPort("portProxy");

    @Rule
    public final DynamicPort httpPortMockServer = new DynamicPort("portMockServer");

    private MuleContext mockServerContext;

    @Override
    protected String getConfigFile()
    {
        return "wsdlAndXsdMockServer/proxy-wsdl-rewrite-schema-locations-conf.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        ApplicationContextBuilder applicationContextBuilder = new ApplicationContextBuilder();
        applicationContextBuilder.setApplicationResources(new String[]{"wsdlAndXsdMockServer/proxy-wsdl-rewrite-schema-locations-conf-server.xml"});
        mockServerContext = applicationContextBuilder.build();
        super.doSetUpBeforeMuleContextCreation();
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        super.doTearDownAfterMuleContextDispose();
        if(mockServerContext !=null)
        {
            mockServerContext.dispose();
        }
    }

    @Test
    public void testProxyWSDLRewriteAllSchemaLocations() throws Exception
    {
        String proxyAddress = "http://localhost:" + httpPortProxy.getNumber() + "/localServicePath";
        MuleMessage response = muleContext.getClient().send(proxyAddress + "?wsdl", null, null);

        Set<String> expectedParametersValues = new HashSet<String>();
        expectedParametersValues.addAll(Arrays.asList("xsd=xsd0"));

        List<Element> schemaImports = getSchemaImports(getWsdl(response));
        for(Element schemaImport : schemaImports)
        {
            String schemaLocation = getLocation(schemaImport);
            int parametersStart = schemaLocation.indexOf("?");
            String locationPath = schemaLocation.substring(0, parametersStart);

            assertEquals(proxyAddress, locationPath);

            String queryString = schemaLocation.substring(parametersStart+1);
            expectedParametersValues.remove(queryString);
        }
        assertTrue(expectedParametersValues.isEmpty());
    }

    private Document getWsdl(MuleMessage response) throws Exception
    {
        return XMLUnit.buildTestDocument(new InputSource(new StringReader(response.getPayloadAsString())));
    }

    private List<Element> getSchemaImports(Document wsdl)
    {
        return DOMUtils.findAllElementsByTagName(wsdl.getDocumentElement(), "xsd:import");
    }

    private String getLocation(Element schemaImport)
    {
        return schemaImport.getAttributes().getNamedItem("schemaLocation").getNodeValue();
    }

}
