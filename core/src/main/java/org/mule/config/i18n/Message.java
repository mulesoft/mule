/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.i18n;

import java.io.Serializable;

public class Message implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6109760447384477924L;

    private String message;
    private int code = 0;
    private Object[] args;
    private Message nextMessage;

    protected Message(String message, int code, Object... args)
    {
        super();
        this.message = message;
        this.code = code;
        this.args = args;
    }

    public int getCode()
    {
        return code;
    }

    public Object[] getArgs()
    {
        return args;
    }

    public String getMessage()
    {
        return message + (nextMessage != null ? ". " + nextMessage.getMessage() : "");
    }

    public Message setNextMessage(Message nextMessage)
    {
        this.nextMessage = nextMessage;
        return this;
    }

    public Message getNextMessage()
    {
        return nextMessage;
    }

    public String toString()
    {
        return this.getMessage();
    }
}
