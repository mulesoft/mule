/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.events;

/**
 * <code>ApplicationEventException</code> TODO
 */

public class ApplicationEventException extends Exception
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 718759087364948708L;

    public ApplicationEventException(String message)
    {
        super(message);
    }

    public ApplicationEventException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
