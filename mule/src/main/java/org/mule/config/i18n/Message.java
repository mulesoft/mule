/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config.i18n;

import java.io.Serializable;

/**
 * <code>Message</code> constructs a
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class Message implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6109760447384477924L;

    public static final int STATIC_ERROR_CODE = -1;

    private static final transient Object[] EMPTY_ARGS = new Object[] {};

    private int code = 0;
    private Object[] args;
    private String message;
    private String bundle = Messages.DEFAULT_BUNDLE;
    private Message nextMessage;

    private Message(String message)
    {
        this.code = STATIC_ERROR_CODE;
        args = EMPTY_ARGS;
        this.message = message;
    }

    public Message(int code)
    {
        this.code = code;
        args = EMPTY_ARGS;
        message = Messages.get(code, args);
    }

    public Message(int code, Object[] args)
    {
        this.code = code;
        this.args = args;
        message = Messages.get(code, args);
    }

    public Message(int code, Object arg1)
    {
        this.code = code;
        if (arg1 == null) {
            arg1 = "null";
        }
        args = new Object[] { arg1 };
        message = Messages.get(code, args);
    }

    public Message(int code, Object arg1, Object arg2)
    {
        this.code = code;
        if (arg1 == null) {
            arg1 = "null";
        }
        if (arg2 == null) {
            arg2 = "null";
        }
        args = new Object[] { arg1, arg2 };
        message = Messages.get(code, args);
    }

    public Message(int code, Object arg1, Object arg2, Object arg3)
    {
        this.code = code;
        if (arg1 == null) {
            arg1 = "null";
        }
        if (arg2 == null) {
            arg2 = "null";
        }
        if (arg3 == null) {
            arg3 = "null";
        }
        args = new Object[] { arg1, arg2, arg3 };
        message = Messages.get(code, args);
    }

    public Message(String bundle, int code)
    {
        this.code = code;
        args = EMPTY_ARGS;
        message = Messages.get(bundle, code, args);
        this.bundle = bundle;
    }

    public Message(String bundle, int code, Object[] args)
    {
        this.code = code;
        this.args = args;
        message = Messages.get(bundle, code, args);
        this.bundle = bundle;
    }

    public Message(String bundle, int code, Object arg1)
    {
        this.code = code;
        if (arg1 == null) {
            arg1 = "null";
        }
        args = new Object[] { arg1 };
        message = Messages.get(bundle, code, args);
        this.bundle = bundle;
    }

    public Message(String bundle, int code, Object arg1, Object arg2)
    {
        this.code = code;
        if (arg1 == null) {
            arg1 = "null";
        }
        if (arg2 == null) {
            arg2 = "null";
        }
        args = new Object[] { arg1, arg2 };
        message = Messages.get(bundle, code, args);
        this.bundle = bundle;
    }

    public Message(String bundle, int code, Object arg1, Object arg2, Object arg3)
    {
        this.code = code;
        if (arg1 == null) {
            arg1 = "null";
        }
        if (arg2 == null) {
            arg2 = "null";
        }
        if (arg3 == null) {
            arg3 = "null";
        }
        args = new Object[] { arg1, arg2, arg3 };
        message = Messages.get(bundle, code, args);
        this.bundle = bundle;
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

    public String getBundle()
    {
        return bundle;
    }

    public static Message createStaticMessage(String message)
    {
        return new Message(message);
    }

    public String toString()
    {
        return getMessage();
    }
}
