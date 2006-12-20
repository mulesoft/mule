/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import org.mule.tck.providers.AbstractMessageAdapterTestCase;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.UMOMessageAdapter;

public class OracleJmsMessageAdapterTestCase extends AbstractMessageAdapterTestCase
{

    public UMOMessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new OracleJmsMessageAdapter(payload);
    }

    public Object getValidMessage() throws Exception
    {
        return OracleJmsConnectorTestCase.getMessage();
    }

}
