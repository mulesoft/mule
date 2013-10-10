/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertThat;

public class JdbcEndpointSessionPropertiesEe2323TestCase extends AbstractJdbcFunctionalTestCase
{

    public JdbcEndpointSessionPropertiesEe2323TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
                {ConfigVariant.FLOW, AbstractJdbcFunctionalTestCase.getConfig() + ",jdbc-endpoint-session-properties.xml"}
        });
    }

    @Test
    public void testSelectOnOutbound() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://Flow1s1", new DefaultMuleMessage(new Object(), muleContext));
        assertThat(response, IsNull.<Object>notNullValue());
        assertThat(response.getExceptionPayload(), IsNull.<Object>nullValue());
    }

}
