/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ProxyValidationComparisonTestCase extends FunctionalTestCase
{
    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    //this request contains no spaces to check the handling of tags following the body
    private static final String ONE_LINER_REQUEST = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                                + "<soap:Body>"
                                                + "<echo xmlns=\"http://www.muleumo.org\">"
                                                + "<echo>hey, there!</echo>"
                                                + "</echo>"
                                                + "</soap:Body>"
                                                + "</soap:Envelope>";

    @Rule
    public final DynamicPort httpPort = new DynamicPort("port1");

    @Parameterized.Parameter
    public String configFile;

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"proxy-validation-comparison-config.xml"},
                {"proxy-validation-comparison-config-httpn.xml"}
        });
    }

    @Override
    protected String getConfigFile()
    {
        return configFile;
    }

    @Test
    public void responsesAreEqualWithAndWithoutValidationEnvelope() throws Exception
    {
        testResponsesWithPayload("envelope");
    }

    @Test
    public void responsesAreEqualWithAndWithoutValidationBody() throws Exception
    {
        testResponsesWithPayload("body");
    }

    private void testResponsesWithPayload(String payload) throws Exception
    {
        MuleMessage responseWithValidation = getResponseFor(payload +"Validation");
        MuleMessage responseWithNoValidation = getResponseFor(payload + "NoValidation");

        assertXMLEqual(responseWithValidation.getPayloadAsString(), responseWithNoValidation.getPayloadAsString());
    }

    private MuleMessage getResponseFor(String path) throws MuleException
    {
        return muleContext.getClient().send(String.format("http://localhost:%s/services/%s", httpPort.getNumber(), path), getTestMuleMessage(ONE_LINER_REQUEST), HTTP_REQUEST_OPTIONS);
    }

}
