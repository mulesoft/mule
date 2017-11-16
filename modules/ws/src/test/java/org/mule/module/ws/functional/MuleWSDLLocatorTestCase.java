/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;

import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.module.ws.consumer.MuleWSDLLocator;
import org.mule.module.ws.consumer.MuleWSDLLocatorConfig;

public class MuleWSDLLocatorTestCase
{

    /**
     * This is a test to verify that the normalization of a path
     * will not fail in environments where the file separator
     * is not the UNIX separator.
     * This test will not fail in linux even if the normalization
     * is breaking the archive file:jar or file:zip spec.
     * 
     * @throws MuleException an exception in case the test fail.
     */
    @Test
    public void testResourceInArchivePathResolution() throws MuleException
    {
        MuleWSDLLocatorConfig locatorConfig = new MuleWSDLLocatorConfig.Builder()
                    .setBaseURI("/")
                    .build();
        MuleWSDLLocator wsdlLocator = new MuleWSDLLocator(locatorConfig);
        wsdlLocator.getImportInputSource("jar:file:./src/test/resources/wsdl.jar!/wsdl/Dummy.wsdl", "Test.wsdl");
    }
}
