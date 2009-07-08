/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.store;

/**
 * A marker interface used to trigger post-deserialization initialization of an object. This works 
 * in the same way as {@link Cloneable} interface. Implementors of this interface must add the 
 * method <code>private void initAfterDeserialization(MuleContext muleContext) throws MuleException</code>
 * to their class (note that it's private). This will get invoked after the object has been 
 * deserialized passing in the current mulecontext when using either 
 * {@link org.mule.transformer.wire.SerializationWireFormat}, 
 * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat}, or the 
 * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
 *
 * @see org.mule.transformer.simple.ByteArrayToSerializable
 * @see org.mule.transformer.wire.SerializationWireFormat
 * @see org.mule.transformer.wire.SerializedMuleMessageWireFormat
 */
public interface DeserializationPostInitialisable
{
    //private void initAfterDeserialisation(MuleContext muleContext) throws MuleException;
}
