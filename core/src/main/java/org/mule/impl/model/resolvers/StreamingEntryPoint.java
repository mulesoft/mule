/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.resolvers;

import org.mule.impl.NoSatisfiableMethodsException;
import org.mule.impl.TooManySatisfiableMethodsException;
import org.mule.impl.VoidResult;
import org.mule.impl.model.streaming.DeferredOutputStream;
import org.mule.impl.model.streaming.StreamingService;
import org.mule.providers.streaming.StreamMessageAdapter;
import org.mule.umo.UMOEventContext;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Will discover the correct entrypoint to invoke when an event is received. The
 * follow are checked for - 1. If the component implements
 * {@link org.mule.impl.model.streaming.StreamingService} this will be used 2. It
 * will look of a method that accepts an {@link java.io.InputStream} and
 * {@link java.io.OutputStream} 3. It will look for a method that accepts an
 * {@link java.io.InputStream} only If none of these criteria are meet, and exception
 * will be thrown.
 */
public class StreamingEntryPoint implements UMOEntryPoint
{
    private Method streamingMethod;
    private boolean inAndOut = false;

    public void initialise(Object component) throws Exception
    {
        if (component instanceof StreamingService)
        {
            streamingMethod = StreamingService.class.getMethods()[0];
        }
        else
        {
            inAndOut = true;
            List methods = ClassUtils.getSatisfiableMethods(component.getClass(), new Class[]{
                InputStream.class, OutputStream.class}, true, false, null);

            if (methods.size() == 0)
            {
                inAndOut = false;
                methods = ClassUtils.getSatisfiableMethods(component.getClass(),
                    new Class[]{InputStream.class}, true, false, null);
            }

            if (methods.size() == 0)
            {
                throw new NoSatisfiableMethodsException(component, new Class[]{InputStream.class},
                    new NoSatisfiableMethodsException(component, new Class[]{InputStream.class,
                        OutputStream.class}));
            }
            else if (methods.size() > 1)
            {
                throw new TooManySatisfiableMethodsException(component, new Class[]{InputStream.class,
                    OutputStream.class});
            }
            else
            {
                streamingMethod = (Method)methods.get(0);
            }
        }
    }

    public Object invoke(Object component, UMOEventContext context) throws Exception
    {
        if (streamingMethod == null)
        {
            initialise(component);
        }

        StreamMessageAdapter adapter = (StreamMessageAdapter)context.getMessage().getAdapter();
        OutputStream out = new DeferredOutputStream(context);

        try
        {
            Object result;

            if (component instanceof StreamingService)
            {
                result = streamingMethod.invoke(component, new Object[]{adapter.getInputStream(), out,
                    context});
            }
            else if (inAndOut)
            {
                result = streamingMethod.invoke(component, new Object[]{adapter.getInputStream(), out});
            }
            else
            {
                result = streamingMethod.invoke(component, new Object[]{adapter.getInputStream()});
            }

            if (streamingMethod.getReturnType().equals(Void.TYPE))
            {
                result = VoidResult.getInstance();
            }

            return result;
        }
        finally
        {
            try
            {
                out.flush();
            }
            catch (IOException e)
            {
                // ignore
                // TODO MULE-863: Why?
            }
        }
    }
}
