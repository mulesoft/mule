/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;

import java.io.OutputStream;

/**
 * A listener used to receive Muleevents from a transport receiver. The listener can be
 * swapped out to deliver message to other frameworks, bypassing the Mule container.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface InternalMessageListener
{
    public UMOMessage onMessage(UMOMessage message, UMOTransaction trans, boolean synchronous, OutputStream outputStream) throws UMOException;

}
