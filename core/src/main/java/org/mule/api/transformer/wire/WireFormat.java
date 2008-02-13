/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transformer.wire;

import org.mule.api.MuleException;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO
 */

public interface WireFormat
{
    Object read(InputStream is) throws MuleException;

    void write(OutputStream out, Object o, String encoding) throws MuleException;

    void setTransferObjectClass(Class clazz);
}
