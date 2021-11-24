/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import org.junit.Rule;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.transformer.DataType;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.api.config.MuleProperties.MULE_DISABLE_SET_VARIABLE_INHERITED_MIME_TYPE;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

public class MULE19941TestCase extends FunctionalTestCase
{

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS =
            newOptions().method(POST.name()).build();

    @Override
    protected String getConfigFile()
    {
        return "set-variable-mime-type-config.xml";
    }

    @Rule
    public SystemProperty setVariableInheritedMimeTypeDisabledProperty =
            new SystemProperty(MULE_DISABLE_SET_VARIABLE_INHERITED_MIME_TYPE, "true");

    @Test
    public void setsContentTypeOnPayloadWithoutMimeTypeOnVariable() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "application/json");
        MuleMessage response = client.send("vm://testInput2",
                new DefaultMuleMessage(TEST_MESSAGE, props, muleContext), HTTP_REQUEST_OPTIONS);

        DataType dataType = (DataType) response.getPayload();

        assertThat(dataType.getMimeType(), equalTo("*/*"));
    }
}
