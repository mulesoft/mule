/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.usecases;

import org.mule.umo.lifecycle.Callable;
import org.mule.umo.UMOEventContext;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ReceiverComponent implements Callable
{
    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        return "Received: " + eventContext.getMessageAsString();
    }
}
