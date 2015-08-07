/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

import org.junit.Test;

public class JdbcSerializableMuleEventTestCase extends AbstractJdbcFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "jdbc-serializable-mule-event.xml";
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
            event.getMuleContext().getObjectSerializer().serialize(event);
            return super.process(event);
        }

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return src;
        }
    }
}
