/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.util.xa.ResourceManagerException;

public interface QueueSession
{

    Queue getQueue(String name);

    void begin() throws ResourceManagerException;

    void commit() throws ResourceManagerException;

    void rollback() throws ResourceManagerException;

}
