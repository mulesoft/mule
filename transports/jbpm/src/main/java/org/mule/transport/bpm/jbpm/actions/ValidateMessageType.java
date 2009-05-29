/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm.jbpm.actions;

import org.mule.util.ClassUtils;

import org.jbpm.JbpmException;
import org.jbpm.graph.exe.ExecutionContext;

/**
 * Throws an exception if the incoming message's class is not as expected.
 * 
 *  &lt;action class="org.mule.transport.bpm.jbpm.actions.ValidateMessageType"&gt;
 *     &lt;expectedType&gt;com.mycompany.MyClass&lt;/expectedType&gt; 
 *  &lt;/action&gt;
 */
public class ValidateMessageType extends IntegrationActionHandler
{

    private static final long serialVersionUID = 1L;

    protected String expectedType;
    protected boolean strict = false;

    public void execute(ExecutionContext executionContext) throws Exception
    {
        super.execute(executionContext);
        Object message = getIncomingMessage();
        if (message == null)
        {
            throw new JbpmException("Incoming message is null.");
        }

        Class expectedClass = ClassUtils.loadClass(expectedType, this.getClass());
        boolean match;
        if (strict)
        {
            match = message.getClass().equals(expectedClass);
        }
        else
        {
            match = expectedClass.isAssignableFrom(message.getClass());
        }
        if (!match)
        {
            throw new JbpmException("Incoming message type is " + message.getClass() + ", expected type is "
                            + expectedType);
        }
    }
}
