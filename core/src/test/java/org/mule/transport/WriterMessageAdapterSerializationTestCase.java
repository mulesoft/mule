/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.transport.MessageAdapter;

public class WriterMessageAdapterSerializationTestCase extends AbstractMessageAdapterSerializationTestCase
{

    @Override
    protected MessageAdapter createMessageAdapter() throws Exception
    {
        WriterMessageAdapter messageAdapter = new WriterMessageAdapter(PAYLOAD);
        messageAdapter.setProperty(STRING_PROPERTY_KEY, STRING_PROPERTY_VALUE);
        return messageAdapter;
    }

}
