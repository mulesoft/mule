/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import org.mule.umo.provider.UMOMessageAdapter;

/**
 * <code>UMOMessage</code> represents a message payload. The Message comprises of
 * the payload itself and properties associated with the payload.
 */

public interface UMOMessage extends UMOMessageAdapter
{

    UMOMessageAdapter getAdapter();

}
