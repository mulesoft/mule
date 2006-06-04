/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.extras.client.MuleClient;
import org.mule.impl.message.ExceptionMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ExceptionListenerTestCase extends FunctionalTestCase
{
    protected String getConfigResources() {
        return "org/mule/test/integration/exceptions/exception-listener-config.xml";
    }

    public void testExceptionStrategy() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage message = client.receive("vm://error.queue", 2000);
        assertNull(message);

        client.send("vm://mycomponent", "test", null);

        message = client.receive("vm://mycomponent.out", 2000);
        assertNull(message);

        message = client.receive("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);
    }
}
