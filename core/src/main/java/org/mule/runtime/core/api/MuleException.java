/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <code>MuleException</code> is the base exception type for the Mule server any
 * other exceptions thrown by Mule code will be based on this exception,
 */
public abstract class MuleException extends Exception
{
    private static final String EXCEPTION_MESSAGE_DELIMITER = StringUtils.repeat('*', 80) + SystemUtils.LINE_SEPARATOR;
    private static final String EXCEPTION_MESSAGE_SECTION_DELIMITER = StringUtils.repeat('-', 80) + SystemUtils.LINE_SEPARATOR;

    private final Map info = new HashMap();
    private int errorCode = -1;
    private String message = null;
    private Message i18nMessage;

    /**
     * @param message the exception message
     */
    public MuleException(Message message)
    {
        super();
        setMessage(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public MuleException(Message message, Throwable cause)
    {
        super(ExceptionHelper.unwrap(cause));
        setMessage(message);
    }

    public MuleException(Throwable cause)
    {
        super(ExceptionHelper.unwrap(cause));
        if (cause != null)
        {
            setMessage(MessageFactory.createStaticMessage(cause.getMessage() + " ("
                                                          + cause.getClass().getName() + ")"));
        }
        else
        {
            initialise();
        }
    }

    protected MuleException()
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
        if (i18nMessage == null)
        {
            i18nMessage = MessageFactory.createStaticMessage(message);
        }
    }

    public int getExceptionCode()
    {
        return errorCode;
    }

    public Message getI18nMessage()
    {
        return i18nMessage;
    }

    public int getMessageCode()
    {
        return (i18nMessage == null ? 0 : i18nMessage.getCode());
    }

    public void addInfo(String name, Object info)
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

    @Override
    public final String getMessage()
    {
        return message;
    }

    protected void initialise()
    {
        setExceptionCode(ExceptionHelper.getErrorCode(getClass()));
    }

    public String getDetailedMessage()
    {
        if (DefaultMuleConfiguration.isVerboseExceptions())
        {
            return getVerboseMessage();
        }
        else
        {
            return getSummaryMessage();
        }
    }

    public String getVerboseMessage()
    {
        MuleException e = ExceptionHelper.getRootMuleException(this);
        if (!e.equals(this))
        {
            return getMessage();
        }
        StringBuilder buf = new StringBuilder(1024);
        buf.append(SystemUtils.LINE_SEPARATOR).append(EXCEPTION_MESSAGE_DELIMITER);
        buf.append("Message               : ").append(message).append(SystemUtils.LINE_SEPARATOR);

        Map info = ExceptionHelper.getExceptionInfo(this);
        for( Map.Entry entry : (Set<Map.Entry>)info.entrySet() )
        {
            String s = (String) entry.getKey();
            int pad = 22 - s.length();
            buf.append(s);
            if (pad > 0)
            {
                buf.append(StringUtils.repeat(' ', pad));
            }
            buf.append(": ");
            buf.append((entry.getValue() == null ? "null" : entry.getValue().toString().replaceAll(SystemUtils.LINE_SEPARATOR, SystemUtils.LINE_SEPARATOR + StringUtils.repeat(' ', 24))))
               .append(SystemUtils.LINE_SEPARATOR);
        }

        // print exception stack
        buf.append(EXCEPTION_MESSAGE_SECTION_DELIMITER);
        buf.append(CoreMessages.rootStackTrace()).append(SystemUtils.LINE_SEPARATOR);
        Throwable root = ExceptionHelper.getRootException(this);
        StringWriter w = new StringWriter();
        PrintWriter p = new PrintWriter(w);
        root.printStackTrace(p);
        buf.append(w.toString()).append(SystemUtils.LINE_SEPARATOR);
        buf.append(EXCEPTION_MESSAGE_DELIMITER);

        return buf.toString();
    }

    public String getSummaryMessage()
    {
        MuleException e = ExceptionHelper.getRootMuleException(this);
        if (!e.equals(this))
        {
            return getMessage();
        }
        StringBuilder buf = new StringBuilder(1024);
        buf.append(SystemUtils.LINE_SEPARATOR).append(EXCEPTION_MESSAGE_DELIMITER);
        buf.append("Message               : ").append(message).append(SystemUtils.LINE_SEPARATOR);
        buf.append("Element               : ").append(ExceptionHelper.getExceptionInfo(this).get(LocatedMuleException.INFO_LOCATION_KEY)).append(SystemUtils.LINE_SEPARATOR);
        
        // print exception stack
        buf.append(EXCEPTION_MESSAGE_SECTION_DELIMITER);
        buf.append(CoreMessages.exceptionStackIs()).append(SystemUtils.LINE_SEPARATOR);
        buf.append(ExceptionHelper.getExceptionStack(this));
        buf.append(SystemUtils.LINE_SEPARATOR)
           .append("  (set debug level logging or '-Dmule.verbose.exceptions=true' for everything)")
           .append(SystemUtils.LINE_SEPARATOR);
        buf.append(EXCEPTION_MESSAGE_DELIMITER);

        return buf.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof MuleException))
        {
            return false;
        }

        final MuleException exception = (MuleException) o;

        if (errorCode != exception.errorCode)
        {
            return false;
        }
        if (i18nMessage != null ? !i18nMessage.equals(exception.i18nMessage) : exception.i18nMessage != null)
        {
            return false;
        }
        if (message != null ? !message.equals(exception.message) : exception.message != null)
        {
            return false;
        }

        return true;
    }

    @Override
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
