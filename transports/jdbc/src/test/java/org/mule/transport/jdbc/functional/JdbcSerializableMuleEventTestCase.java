/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.util.SerializationUtils;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JdbcSerializableMuleEventTestCase extends AbstractJdbcFunctionalTestCase
{

    public JdbcSerializableMuleEventTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.FLOW, "jdbc-serializable-mule-event.xml"}
        });
    }

    @Test
    public void serializesJdbcInboundMuleEvent() throws Exception
    {
        MuleMessage response = muleContext.getClient().request("vm://testOut", RECEIVE_TIMEOUT);
        assertNotNull(response);
    }


    public static final class AssertSerializableEventTransformer extends AbstractTransformer
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            SerializationUtils.serialize(event);
            return super.process(event);
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return src;
        }
    }
}
