/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.compression;

import java.io.IOException;

/**
 * <code>CompressionStrategy</code> is a base interface for Different compression
 * strategies
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface CompressionStrategy
{
    /**
     * The fully qualified class name of the fallback
     * <code>CompressionStrategy</code> implementation class to use, if no other
     * can be found. the default is
     * <code>org.mule.util.compression.GZipCompression</code>
     */
    String COMPRESSION_DEFAULT = "org.mule.util.compression.GZipCompression";

    /**
     * JDK1.3+ 'Service Provider' specification (
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html )
     */
    String SERVICE_ID = "META-INF/services/org.mule.util.compression.CompressionStrategy";

    byte[] compressByteArray(byte[] bytes) throws IOException;

    byte[] uncompressByteArray(byte[] bytes) throws IOException;

    boolean isCompressed(byte[] bytes) throws IOException;
}
