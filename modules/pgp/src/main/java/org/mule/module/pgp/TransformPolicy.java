/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


