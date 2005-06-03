/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.umo;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.util.StringMessageHelper;

/**
 * <code>UMOException</code> is the base exception type for the Mule server
 * any other exceptions thrown by Mule code will be based on this exception
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class UMOException extends Exception
{
    private Map info = new HashMap();
    private int errorCode = -1;
    private String message = null;
    private Message i18nMessage;

    /**
     * @param message the exception message
     */
    public UMOException(Message message)
    {
        super();
        setMessage(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public UMOException(Message message, Throwable cause)
    {
        super((cause instanceof InvocationTargetException ? ((InvocationTargetException) cause).getTargetException()
                : cause));
        setMessage(message);
    }

    public UMOException(Throwable cause)
    {
        super((cause instanceof InvocationTargetException ? ((InvocationTargetException) cause).getTargetException()
                : cause));
        setMessage(Message.createStaticMessage(cause.getMessage() + " (" + cause.getClass().getName() + ")"));
        initialise();
    }

    protected UMOException()
    {
        super();
        initialise();
    }

    protected void setMessage(Message message)
    {
        initialise();
        this.message = message.getMessage();
        i18nMessage = message;
    }

    protected void setMessage(String message)
    {
        initialise();
        this.message = message;
        if (i18nMessage == null) {
            i18nMessage = Message.createStaticMessage(message);
        }
    }

    public int getExceptionCode()
    {
        return errorCode;
    }

    public int getMessageCode()
    {
        return (i18nMessage == null ? 0 : i18nMessage.getCode());
    }

    public void addInfo(String name, String info)
    {
        this.info.put(name, info);
    }

    protected void appendMessage(String s)
    {
        message += s;
    }

    protected void prependMessage(String s)
    {
        message = message + ". " + s;
    }

    protected void setExceptionCode(int code)
    {
        errorCode = code;
    }

    public final String getMessage()
    {
        return message;
    }

    protected void initialise()
    {
        setExceptionCode(ExceptionHelper.getErrorCode(getClass()));
        String javadoc = ExceptionHelper.getJavaDocUrl(getClass());
        String doc = ExceptionHelper.getDocUrl(getClass());
        if (javadoc != null) {
            info.put("JavaDoc", javadoc);
        }
        if (doc != null) {
            info.put("Other Doc", doc);
        }
    }

    public String getDetailedMessage()
    {
        UMOException e = ExceptionHelper.getRootMuleException(this);
        if (!e.equals(this)) {
            return getMessage();
        }
        StringBuffer buf = new StringBuffer();
        buf.append("\n").append(StringMessageHelper.charString('*', 80)).append("\n");
        buf.append("Message          : ").append(message).append("\n");
        buf.append("Type             : ").append(getClass().getName()).append("\n");
        buf.append("Code             : ").append(getExceptionCode() + getMessageCode()).append("\n");
        // buf.append("Msg Code : ").append(getMessageCode()).append("\n");
        for (Iterator iterator = info.keySet().iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            int pad = 17 - s.length();
            buf.append(s);
            if (pad > 0) {
                buf.append(StringMessageHelper.charString(' ', pad));
            }
            buf.append(": ");
            buf.append(info.get(s)).append("\n");
        }

        // print exception stack
        buf.append(StringMessageHelper.charString('*', 80)).append("\n");
        buf.append(new Message(Messages.EXCEPTION_STACK_IS)).append("\n");
        buf.append(ExceptionHelper.getExceptionStack(this));

        buf.append(StringMessageHelper.charString('*', 80)).append("\n");
        buf.append(new Message(Messages.ROOT_STACK_TRACE)).append("\n");
        Throwable root = ExceptionHelper.getRootException(this);
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        root.printStackTrace(p);
        buf.append(w.toString()).append("\n");
        buf.append(StringMessageHelper.charString('*', 80)).append("\n");

        return buf.toString();
    }

    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UMOException)) {
            return false;
        }

        final UMOException umoException = (UMOException) o;

        if (errorCode != umoException.errorCode) {
            return false;
        }
        if (i18nMessage != null ? !i18nMessage.equals(umoException.i18nMessage) : umoException.i18nMessage != null) {
            return false;
        }
        if (message != null ? !message.equals(umoException.message) : umoException.message != null) {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = errorCode;
        result = 29 * result + (message != null ? message.hashCode() : 0);
        result = 29 * result + (i18nMessage != null ? i18nMessage.hashCode() : 0);
        return result;
    }

    public Map getInfo()
    {
        return info;
    }
}
