/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.session.SerializeAndEncodeSessionHandler;

/**
 * @deprecated Use an explicit SessionHandler such as SerializeAndEncodeSessionHandler or SerializeOnlySessionHandler
 */
public class MuleSessionHandler extends SerializeAndEncodeSessionHandler
{
    // deprecated
}
