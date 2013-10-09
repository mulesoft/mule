/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A writer of {@link OutputStream}
 */
public interface StreamTransformer
{
    /**
     * Initialize this writer to write in the out OutputStream
     *
     * @param out the OutputStream where this writer is going to write information
     * @throws Exception
     */
    void initialize(OutputStream out) throws Exception;

    /**
     * Writes into out the number of bytes requested
     *
     * @param out the OutputStream where this writer is going to write information
     * @param bytesRequested how many bytes this writer needs to write
     * @return whether this writer has finished writing (no more bytes need to be written)
     * @throws Exception
     */
    boolean write(OutputStream out, AtomicLong bytesRequested) throws Exception;
}
