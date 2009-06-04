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

import org.mule.DefaultMuleMessage;
import org.mule.api.transport.MessageAdapter;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Not all {@link MessageAdapter}s can be serialized using the native Java serialization as their
 * paylod is not {@link Serializable}. These message adapters can implement this interface
 * to help with the serialization of the paylod: the raw bytes are persisted when the message 
 * adapter is serialized. Upon deserialization a {@link DefaultMessageAdapter} is created to hold
 * this payload.
 * 
 * @see DefaultMuleMessage#writeObject(ObjectOutputStream)
 * @see DefaultMuleMessage#readObject(ObjectInputStream)
 */
public interface MessageAdapterSerialization
{
    enum Type
    {
        /**
         * Use regular Java serialization to marshal the MessageAdapter
         */
        DefaultSerialization, 
        
        /**
         * Delegate serialization to the MessageAdapter
         */
        CustomSerialization;
    }

    /**
     * @return The payload of this MessageAdapter as bytes.
     */
    byte[] getPayloadForSerialization() throws Exception;
}


