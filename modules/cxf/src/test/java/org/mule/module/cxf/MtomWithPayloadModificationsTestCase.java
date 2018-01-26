/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;


import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import javax.activation.DataSource;

import org.junit.Rule;
import org.junit.Test;

public class MtomWithPayloadModificationsTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("httpPort");

    @Override
    protected String getConfigFile()
    {
        return "mtom-payload-modifications-config.xml";
    }

    @Test
    public void testWithPayloadModifications() throws Exception
    {
        String body = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("./direct/direct-request.xml"));
        MuleEvent request = getTestEvent(body);
        request.getMessage().addOutboundAttachment("body", body, "text/xml");
        MuleEvent response = runFlow("testMtomWithPayloadModifications", request);
        DataSource bodyDataSource = response.getMessage().getInboundAttachment("<root.message@cxf.apache.org>").getDataSource();

        assertThat(IOUtils.toString(bodyDataSource.getInputStream()), is(body));
    }

}

