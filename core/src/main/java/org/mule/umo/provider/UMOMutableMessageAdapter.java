/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.provider;

/**
 *  Defines a {@link org.mule.umo.provider.UMOMessageAdapter} that can have its payload re-assigned after it
 * has been created. Transport message adapters must never use this interface since a message payload received
 * from a transport is always consdered read-only.
 */

public interface UMOMutableMessageAdapter extends UMOMessageAdapter
{
    /**
     * Update the message payload. This is typically only called if the
     * payload was originally an InputStream. In which case, if the InputStream
     * is consumed, it needs to be replaced for future access.
     *
     * @param payload the object to assign as the message payload
     */
    void setPayload(Object payload);
}
