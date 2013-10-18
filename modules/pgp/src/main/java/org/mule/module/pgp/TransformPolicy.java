/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

/**
 * A {@link TransformPolicy} represents a policy that controls how {@link StreamTransformer}
 * transform {@link LazyTransformedInputStream}.
 * 
 * For instance, a policy would be transform all the bytes of the stream without waiting for some
 * object to be requested. 
 */
public interface TransformPolicy
{
    /**
     * Initialize this policy with the corresponding lazyTransformedInputStream
     * 
     * @param lazyTransformedInputStream
     */
    void initialize(LazyTransformedInputStream lazyTransformedInputStream);

    /**
     * Releases all the resources of this policy
     */
    void release();

    /**
     * Notifies this policy that the object has requested nroOfBytes
     * 
     * @param nroOfBytes the nro of bytes requested by the object
     */
    void readRequest(long nroOfBytes);
}


