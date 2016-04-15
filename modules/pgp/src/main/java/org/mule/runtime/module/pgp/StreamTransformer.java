/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
