/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.StreamCorruptedException;

import org.apache.commons.io.input.ClassLoaderObjectInputStream;


public class ClassSpecificObjectInputStream extends ClassLoaderObjectInputStream
{

    private Class classToAccept;

    private boolean firstClass = true;

    /**
     * Constructs a new ClassLoaderObjectInputStream.
     *
     * @param classLoader the ClassLoader from which classes should be loaded
     * @param inputStream the InputStream to work on
     * @throws IOException              in case of an I/O error
     * @throws StreamCorruptedException if the stream is corrupted
     */
    public ClassSpecificObjectInputStream(ClassLoader classLoader, InputStream inputStream, Class classToAccept) throws IOException, StreamCorruptedException
    {
        super(classLoader, inputStream);
        this.classToAccept = classToAccept;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass objectStreamClass) throws IOException, ClassNotFoundException
    {
        Class resolved = super.resolveClass(objectStreamClass);

        if (!firstClass)
        {
            // if it is not the first class seen, it is a field and we shouldn't filter it
            return resolved;
        }

        if (classToAccept.isAssignableFrom(resolved))
        {
            firstClass = false;
            return resolved;
        }

        throw new InvalidClassException(format("Expecting an instance of '%s' but '%s' found", classToAccept.getName(), resolved.getName()));
    }

    public static class Provider implements ObjectInputStreamProvider
    {

        private Class classToAccept;

        public Provider(Class classToAccept)
        {
            this.classToAccept = classToAccept;
        }

        @Override
        public ObjectInputStream get(ClassLoader classLoader, InputStream inputStream) throws IOException
        {
            return new ClassSpecificObjectInputStream(classLoader, inputStream, classToAccept);
        }
    }

}
