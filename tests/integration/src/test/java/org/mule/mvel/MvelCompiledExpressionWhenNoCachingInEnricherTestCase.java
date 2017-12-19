/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.mvel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.api.config.MuleProperties.SYSTEM_PROPERTY_PREFIX;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

public class MvelCompiledExpressionWhenNoCachingInEnricherTestCase extends FunctionalTestCase
{

    private static final String RESPONSE = "dummyValue";

    @Rule
    public SystemProperty noCache = new SystemProperty(SYSTEM_PROPERTY_PREFIX + "disableMelExpressionCache", "true");

    @Override
    protected String getConfigFile()
    {
        return "mvel-compiled-expression-no-caching-in-enricher.xml";
    }

    @Test
    public void dataTypesCorrectlyPropagated() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://enricher", TEST_MESSAGE, null);

        assertThat(response.getPayloadAsString(), equalTo(RESPONSE));
    }
}
