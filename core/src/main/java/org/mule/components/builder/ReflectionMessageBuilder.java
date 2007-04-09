/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.builder;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.NoSatisfiableMethodsException;
import org.mule.impl.TooManySatisfiableMethodsException;
import org.mule.umo.UMOMessage;
import org.mule.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Will try and set the result of an invocation as a bean property on the request
 * message using reflection.
 */
public class ReflectionMessageBuilder extends AbstractMessageBuilder
{

    // we don't want to match these methods when looking for a method
    protected final Set ignoreMethods = new HashSet(Arrays.asList(new String[]{"equals", "getInvocationHandler"}));

    public Object buildMessage(UMOMessage request, UMOMessage response) throws MessageBuilderException
    {
        Object master = request.getPayload();
        Object property = response.getPayload();
        List methods = null;
        try
        {
            methods = ClassUtils.getSatisfiableMethods(master.getClass(), new Class[]{property.getClass()},
                true, false, ignoreMethods);
        }
        catch (Exception e)
        {
            throw new MessageBuilderException(request, e);
        }
        if (methods.size() == 0)
        {
            throw new MessageBuilderException(request,
                    new NoSatisfiableMethodsException(master, new Class[]{property.getClass()}));
        }
        else if (methods.size() > 1)
        {
            throw new MessageBuilderException(request,
                    new TooManySatisfiableMethodsException(master,
                            new String[]{property.getClass().getName()}));
        }
        else
        {
            Method m = (Method) methods.get(0);
            try
            {
                m.invoke(master,
                    (property.getClass().isArray() ? (Object[]) property : new Object[]{property}));
            }
            catch (Exception e)
            {
                throw new MessageBuilderException(
                    new Message(Messages.FAILED_TO_INVOKE_X, m.getName() + " on " + master.getClass().getName()), 
                    request, e);

            }
        }
        return master;
    }
}
