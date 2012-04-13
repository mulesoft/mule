/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;


import org.junit.Test;

public class ExpressionSplitterPropertiesTestCase extends FunctionalTestCase
{
    private static final String INPUT = "<message>\n" +
                                        "\t<document>\n" +
                                        "\t\t<partone>\n" +
                                        "\t\t\tpartone1\n" +
                                        "\t\t</partone>\n" +
                                        "\t\t<parttwo>\n" +
                                        "\t\t\tparttwo1\n" +
                                        "\t\t</parttwo>\n" +
                                        "\t</document>\n" +
                                        "\t<document>\n" +
                                        "\t\t<partone>\n" +
                                        "\t\t\tpartone2\n" +
                                        "\t\t</partone>\n" +
                                        "\t\t<parttwo>\n" +
                                        "\t\t\tpartone2\n" +
                                        "\t\t</parttwo>\n" +
                                        "\t</document>\n" +
                                        "</message>";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-splitter-properties-conf.xml";
    }

    @Test
    public void testExpressionSplitterPropertiesPropagation() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(INPUT, muleContext);
        MuleMessageCollection result = (MuleMessageCollection) client.send("vm://in", message);

        assertNotNull(result.getSessionProperty("propSession"));
        assertNotNull(result.getSessionProperty("responsePropSession"));
        assertNotNull(result.getSessionProperty("responsePropInvk"));
        assertNotNull(result.getSessionProperty("splitterPropSession"));
        assertNotNull(result.getSessionProperty("splitterPropInvk"));
        assertNotNull(result.getSessionProperty("splitterPropOut"));
        assertNotNull(result.getSessionProperty("aggregatorPropSession"));
        assertNotNull(result.getSessionProperty("aggregatorPropInvk"));

    }

}
