/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.usecases.dlq;

import org.mule.extras.client.MuleClient;
import org.mule.impl.message.ExceptionMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import javax.jms.ObjectMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DLQExceptionHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "exception-dlq.xml";
    }

    public void testDLQ() throws Exception
    {
        MuleClient client = new MuleClient();
        client.dispatch("jms://request.queue", "testing 1 2 3", null);

        UMOMessage message = client.receive("jms://out.queue", 3000);
        assertNull(message);

        message = client.receive("jms://DLQ", 500000000);
        assertNotNull(message);

        ObjectMessage m = (ObjectMessage) message.getPayload();
        ExceptionMessage em = (ExceptionMessage) m.getObject();
        assertEquals("testing 1 2 3", em.getPayload());
    }
}
