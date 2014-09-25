/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.fruit;

import org.mule.tck.testmodels.fruit.Apple;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

@Provider
public class AppleBodyWriter implements MessageBodyWriter<Apple>
{

    @Override
    public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType)
    {
        return aClass == Apple.class;
    }

    @Override
    public long getSize(Apple apple, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType)
    {
        return 0;
    }

    @Override
    public void writeTo(Apple apple, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> stringObjectMultivaluedMap, OutputStream outputStream) throws IOException, WebApplicationException
    {
        String response = String.format("The apple %s bitten but %s", apple.isBitten() ? "is" : "is not", apple.isWashed() ? "clean" : "dirty");
        outputStream.write(response.getBytes());
    }
}
