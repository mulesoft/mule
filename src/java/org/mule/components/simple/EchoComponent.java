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
package org.mule.components.simple;

import org.mule.umo.UMOEventContext;

/**
 * <code>EchoComponent</code> will log the message and return the payload back
 * as the result
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class EchoComponent extends LogComponent implements EchoService
{
    public Object onCall(UMOEventContext context) throws Exception
    {
        super.onCall(context);
        return context.getTransformedMessage();
    }

    public String echo(String echo)
    {
        return echo;
    }
}
