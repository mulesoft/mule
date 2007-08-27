/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    protected Message(String message, int code, Object[] args)
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
