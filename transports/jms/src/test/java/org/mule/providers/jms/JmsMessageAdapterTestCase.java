/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

public class JmsMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
     */
    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
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
