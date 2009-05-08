/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.cometd;

import org.mule.transport.AbstractMessageAdapterTestCase;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.MessagingException;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Apple;


import java.util.Map;

public class CometdMessageAdapterObjectTestCase extends AbstractMessageAdapterTestCase
{
    public Object getValidMessage() throws Exception
    {
        return new FruitBowl(new Apple(), new Banana());
    }

    public MessageAdapter createAdapter(Object payload) throws MessagingException
    {
        return new CometdMessageAdapter(payload);
    }
}