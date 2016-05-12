/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.protocols;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.apache.log4j.Logger;

/**
 * A length protocol that uses a specific class loader to load objects from streams
 * 
 * @since 2.2.6
 */
public class CustomClassLoadingLengthProtocol extends LengthProtocol
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private ClassLoader classLoader;

    @Override
    public Object read(InputStream is) throws IOException
    {
        byte[] bytes = (byte[]) super.read(is);

        if (bytes == null)
        {
            return null;
        }
        else
        {
            ClassLoaderObjectInputStream classLoaderIS = new ClassLoaderObjectInputStream(this.getClassLoader(),
                is);
            try
            {
                return classLoaderIS.readObject();
            }
            catch (ClassNotFoundException e)
            {
                logger.warn(e.getMessage());
                IOException iox = new IOException();
                iox.initCause(e);
                throw iox;
            }
            finally
            {
                classLoaderIS.close();
            }
        }
    }

    public ClassLoader getClassLoader()
    {
        if (this.classLoader == null)
        {
            this.classLoader = this.getClass().getClassLoader();
        }
        return classLoader;
    }

    public void setClassLoader(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }
}
