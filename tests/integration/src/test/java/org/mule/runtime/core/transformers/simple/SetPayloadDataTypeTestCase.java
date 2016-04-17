/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.transformers.simple;

import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.LocalMuleClient;
import org.mule.runtime.api.metadata.DataType;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.runtime.core.transformer.types.MimeTypes;

import org.junit.Test;

public class SetPayloadDataTypeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "set-payload-data-type-config.xml";
    }

    @Test
    public void setsPayloadLocal() throws Exception
    {
        doSetPayloadTest("setPayload");
    }

    private void doSetPayloadTest(String flowName) throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        // MuleMessage response = client.send(url, TEST_MESSAGE, null);
        MuleMessage response = flowRunner(flowName).withPayload(TEST_MESSAGE).run().getMessage();

        DataType dataType = response.getDataType();

        assertThat(dataType, DataTypeMatcher.like(String.class, MimeTypes.XML, "UTF-16"));
    }
}
