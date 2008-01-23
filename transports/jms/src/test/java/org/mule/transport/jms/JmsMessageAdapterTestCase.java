/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.MessagingException;
import org.mule.api.transport.MessageAdapter;
import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.transport.jms.JmsConstants;
import org.mule.transport.jms.JmsMessageAdapter;

public class JmsMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{  
    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new JmsMessageAdapter(payload);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
     */
    public Object getValidMessage() throws Exception
    {
        return JmsConnectorTestCase.getMessage();
    }

    public void testIllegalSpecification() throws Exception
    {
        JmsMessageAdapter a = (JmsMessageAdapter)this.createAdapter(this.getValidMessage());

        // these will work
        a.setSpecification(JmsConstants.JMS_SPECIFICATION_102B);
        a.setSpecification(JmsConstants.JMS_SPECIFICATION_11);

        try
        {
            // this will not :)
            a.setSpecification("1.2");
        }
        catch (IllegalArgumentException iax)
        {
            // OK
        }
    }

}
