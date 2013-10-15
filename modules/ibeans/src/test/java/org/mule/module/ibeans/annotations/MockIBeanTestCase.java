/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.annotations;


import org.mule.module.ibeans.HostIpIBean;
import org.mule.module.xml.util.XMLUtils;

import org.ibeans.annotation.MockIntegrationBean;
import org.ibeans.api.CallException;
import org.junit.Test;
import org.w3c.dom.Document;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

public class MockIBeanTestCase extends AbstractIBeansTestCase
{

    public static final String GOOD_IP = "12.215.42.19";
    public static final String BAD_IP = "12.215.42.";

    @SuppressWarnings("unused")
    @MockIntegrationBean
    private HostIpIBean hostip;

    @Test
    public void testSuccessfulHostipLookup() throws Exception
    {
        hostip.init(Document.class);
        when(hostip.getHostInfo(GOOD_IP)).thenAnswer(withXmlData("mock/hostip-found-response.xml", hostip));

        Document result = hostip.getHostInfo(GOOD_IP);
        String loc = XMLUtils.selectValue("//*[local-name()='coordinates']", result);
        assertEquals("-88.4588,41.7696", loc);
    }

    //    @Test
    //    public void testSuccessfulHostipLookupWithReturn() throws Exception
    //    {
    //        hostip.init(Document.class);
    //        when(hostip.hasIp(GOOD_IP)).thenAnswer(withXmlData("mock/hostip-found-response.xml", hostip));
    //        when(hostip.hasIp(GOOD_IP)).thenAnswer(withXmlData("mock/hostip-found-response.xml", hostip));
    //
    //        assertTrue(hostip.hasIp(GOOD_IP));
    //    }

    @Test(expected = CallException.class)
    public void testUnsuccessfulHostipLookup() throws Exception
    {
        //Because we are testing this in the core module we cannot import the xml module, so
        //we set the return type to sting and define a RegEx error filter on the iBean
        hostip.init(String.class);
        when(hostip.getHostInfo(BAD_IP)).thenAnswer(withXmlData("mock/hostip-not-found-response.xml", hostip));

        hostip.getHostInfo(BAD_IP);
    }

    @Test
    public void testTemplateMethod() throws Exception
    {
        String result = hostip.dummyTemplateMethod("three");
        assertEquals("one two three", result);
    }
}
