/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
