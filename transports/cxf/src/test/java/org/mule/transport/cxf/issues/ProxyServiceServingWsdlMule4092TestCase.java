/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.issues;

import org.mule.tck.FunctionalTestCase;
import org.mule.util.IOUtils;
import org.mule.util.SystemUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.custommonkey.xmlunit.XMLUnit;

public class ProxyServiceServingWsdlMule4092TestCase extends FunctionalTestCase
{
    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        // MULE-4667
        return SystemUtils.isIbmJDK();
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Override
    protected String getConfigResources()
    {
        return "issues/proxy-service-serving-wsdl-mule4092.xml";
    }

    public void testProxyServiceWSDL() throws MalformedURLException, IOException, Exception
    {
        assertTrue(compareResults(getXML("issues/test.wsdl"), IOUtils.toString(new URL(
            "http://localhost:8777/services/onlinestore?wsdl").openStream())));
    }

    protected String getXML(String requestFile) throws Exception
    {
        String xml = IOUtils.toString(IOUtils.getResourceAsStream(requestFile, this.getClass()), "UTF-8");
        if (xml != null)
        {
            return xml;
        }
        else
        {
            fail("Unable to load test request file");
            return null;
        }
    }

    protected boolean compareResults(String expected, String result)
    {
        try
        {
            String expectedString = this.normalizeString(expected);
            String resultString = this.normalizeString(result);
            return XMLUnit.compareXML(expectedString, resultString).similar();
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    protected String normalizeString(String rawString)
    {
        rawString = rawString.replaceAll("\r", "");
        rawString = rawString.replaceAll("\n", "");
        return rawString.replaceAll("\t", "");
    }
}
