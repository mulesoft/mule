/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.core.transformers.simple;

import static java.nio.charset.StandardCharsets.UTF_16;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class SetPayloadDataTypeTestCase extends AbstractIntegrationTestCase
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
        MuleMessage response = flowRunner(flowName).withPayload(TEST_MESSAGE).run().getMessage();

        assertThat(response.getDataType(), like(String.class, MediaType.XML, UTF_16));
    }
}
