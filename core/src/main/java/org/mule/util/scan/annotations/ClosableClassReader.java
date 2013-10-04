/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.scan.annotations;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;

/**
 * A Class reader trhat will close the stream once initialised
 */
public class ClosableClassReader extends ClassReader
{
    public ClosableClassReader(String s) throws IOException
    {
        super(s);
    }

    public ClosableClassReader(InputStream inputStream) throws IOException
    {
          super(inputStream);
          inputStream.close();
    }
}
