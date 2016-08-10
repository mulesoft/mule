/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class TransformerOnMessageCollectionTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/transformer-on-message-collection-config.xml";
    }

    @Test
    public void testIssue() throws Exception
    {
        List<String> values = new LinkedList<String>();
        values.add("One");
        values.add("Two");

        MuleMessage response = flowRunner("test").withPayload(values).run().getMessage();
        assertEquals("foo", response.getPayload());
        assertFalse(response.getPayload() instanceof List);
    }
}
