/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.abbreviate;
import static org.mule.util.ClassUtils.isConsumable;

import org.mule.VoidMuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.ExceptionHelper;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.Message;
import org.mule.routing.filters.RegExFilter;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.NullPayload;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * <code>MessagingException</code> is a general message exception thrown when errors
 * specific to Message processing occur..
 */

public class MessagingException extends MuleException
{
    public static final String PAYLOAD_INFO_KEY = "Payload";

    /**
     * Serial version
     */
    private static final long serialVersionUID = 6941498759267936649L;

    /**
     * The MuleMessage being processed when the error occurred
     */
    protected transient MuleMessage muleMessage;

    /**
     * The MuleEvent being processed when the error occurred
     */
    protected final transient MuleEvent event;

    protected transient MuleEvent processedEvent;

    private boolean causeRollback;
    private boolean handled;
    private transient MessageProcessor failingMessageProcessor;

    /**
     * @deprecated use MessagingException(Message, MuleEvent)
     */
    @Deprecated
    public MessagingException(Message message, MuleMessage muleMessage)
    {
        super();
        this.muleMessage = muleMessage;
        this.event = null;
        setMessage(generateMessage(message));
    }

    public MessagingException(Message message, MuleEvent event)
    {
        super();
        this.event = event;
        extractMuleMessage(event);
        setMessage(generateMessage(message));
    }

    public MessagingException(Message message, MuleEvent event, MessageProcessor failingMessageProcessor)
    {
        super();
        this.event = event;
        extractMuleMessage(event);
        this.failingMessageProcessor = failingMessageProcessor;
        setMessage(generateMessage(message));
    }

    /**
     * @deprecated use MessagingException(Message, MuleEvent, Throwable)
     */
    @Deprecated
    public MessagingException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(cause);
        this.muleMessage = muleMessage;
        this.event = null;
        setMessage(generateMessage(message));
    }

    public MessagingException(Message message, MuleEvent event, Throwable cause)
    {
        super(cause);
        this.event = event;
        extractMuleMessage(event);
        setMessage(generateMessage(message));
    }

    public MessagingException(Message message,
                              MuleEvent event,
                              Throwable cause,
                              MessageProcessor failingMessageProcessor)
    {
        super(cause);
        this.event = event;
        extractMuleMessage(event);
        this.failingMessageProcessor = failingMessageProcessor;
        setMessage(generateMessage(message));
    }

    public MessagingException(MuleEvent event, Throwable cause)
    {
        super(cause);
        this.event = event;
        extractMuleMessage(event);
        setMessage(generateMessage(getI18nMessage()));
    }

    public MessagingException(MuleEvent event, Throwable cause, MessageProcessor failingMessageProcessor)
    {
        super(cause);
        this.event = event;
        extractMuleMessage(event);
        this.failingMessageProcessor = failingMessageProcessor;
        setMessage(generateMessage(getI18nMessage()));
    }

    protected String generateMessage(Message message)
    {
        StringBuilder buf = new StringBuilder(80);

        if (message != null)
        {
            buf.append(message.getMessage()).append(".");
        }

        if (muleMessage != null)
        {
            if (DefaultMuleConfiguration.isVerboseExceptions())
            {
                Object payload = muleMessage.getPayload();
                if (payload == null)
                {
                    payload = NullPayload.getInstance();
                }

                if (isConsumable(muleMessage.getPayload().getClass()))
                {
                    addInfo(PAYLOAD_INFO_KEY, abbreviate(payload.toString(), 1000));
                }
                else
                {
                    try
                    {
                        addInfo(PAYLOAD_INFO_KEY, muleMessage.getPayloadAsString());
                    }
                    catch (Exception e)
                    {
                        addInfo(PAYLOAD_INFO_KEY, format("%s while getting payload: %s", e.getClass().getName(), e.getMessage()));
                    }
                }
            }
        }
        else
        {
            buf.append("The current MuleMessage is null! Please report this to ").append(
                MuleManifest.getDevListEmail());
            addInfo(PAYLOAD_INFO_KEY, NullPayload.getInstance().toString());
        }

        return buf.toString();
    }

    /**
     * @deprecated use {@link #getEvent().getMessage()} instead
     */
    @Deprecated
    public MuleMessage getMuleMessage()
    {
        if ((getEvent() != null))
        {
            return event.getMessage();
        }
        return muleMessage;
    }

    /**
     * @return event associated with the exception
     */
    public MuleEvent getEvent()
    {
        return processedEvent != null && !VoidMuleEvent.getInstance().equals(processedEvent)
                                                                                            ? processedEvent
                                                                                            : event;
    }

    /**
     * Sets the event that should be processed once this exception is caught
     * 
     * @param processedEvent event bounded to the exception
     */
    public void setProcessedEvent(MuleEvent processedEvent)
    {
        if (processedEvent != null && !VoidMuleEvent.getInstance().equals(processedEvent))
        {
            this.processedEvent = processedEvent;
            this.muleMessage = this.processedEvent.getMessage();
        }
        else
        {
            this.processedEvent = null;
            this.muleMessage = null;
        }
    }

    /**
     * Evaluates if the exception was caused (instance of) by the provided exception
     * type
     * 
     * @param e exception type to check against
     * @return true if the cause exception is an instance of the provided exception
     *         type
     */
    public boolean causedBy(final Class e)
    {
        if (e == null)
        {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return (ExceptionHelper.traverseCauseHierarchy(this, new ExceptionHelper.ExceptionEvaluator<Object>()
        {
            @Override
            public Object evaluate(Throwable causeException)
            {
                if (e.isAssignableFrom(causeException.getClass()))
                {
                    return causeException;
                }
                return null;
            }
        }) != null);
    }

    /**
     * Evaluates if the exception was caused by the type and only the type provided
     * exception type i,e: if cause exception is NullPointerException will only
     * return true if provided exception type is NullPointerException
     * 
     * @param e exception type to check against
     * @return true if the cause exception is exaclty the provided exception type
     */
    public boolean causedExactlyBy(final Class e)
    {
        if (e == null)
        {
            throw new IllegalArgumentException("Class cannot be null");
        }
        return (ExceptionHelper.traverseCauseHierarchy(this, new ExceptionHelper.ExceptionEvaluator<Object>()
        {
            @Override
            public Object evaluate(Throwable causeException)
            {
                if (causeException.getClass().equals(e))
                {
                    return causeException;
                }
                return null;
            }
        }) != null);
    }

    /**
     * @return the exception thrown by the failing message processor
     */
    public Exception getCauseException()
    {
        Throwable rootException = ExceptionHelper.getRootException(this);
        if (rootException == null)
        {
            rootException = this;
        }
        return (Exception) rootException;
    }

    /**
     * Checks the cause exception type name matches the provided regex. Supports any
     * java regex plus *, * prefix, * sufix
     * 
     * @param regex regular expression to match against the exception type name
     * @return true if the exception matches the regex, false otherwise
     */
    public boolean causeMatches(final String regex)
    {
        if (regex == null)
        {
            throw new IllegalArgumentException("regex cannot be null");
        }
        return (ExceptionHelper.traverseCauseHierarchy(this, new ExceptionHelper.ExceptionEvaluator<Object>()
        {
            @Override
            public Object evaluate(Throwable e)
            {
                WildcardFilter wildcardFilter = new WildcardFilter(regex);
                if (wildcardFilter.accept(e.getClass().getName()))
                {
                    return e;
                }
                try
                {
                    RegExFilter regExFilter = new RegExFilter(regex);
                    if (regExFilter.accept(e.getClass().getName()))
                    {
                        return e;
                    }
                }
                catch (Exception regexEx)
                {
                    // Do nothing, regex such as *, *something, something* will fail,
                    // just don't match
                }
                return null;
            }
        })) != null;
    }

    /**
     * Signals if the exception cause rollback of any current transaction if any or
     * if the message source should rollback incoming message
     * 
     * @return true if exception cause rollback, false otherwise
     */
    public boolean causedRollback()
    {
        return causeRollback;
    }

    /**
     * Marks exception as rollback cause. Useful for message sources that can provide
     * some rollback mechanism.
     * 
     * @param causeRollback
     */
    public void setCauseRollback(boolean causeRollback)
    {
        this.causeRollback = causeRollback;
    }

    /**
     * Marks an exception as handled so it won't be re-throwed
     * 
     * @param handled true if the exception must be mark as handled, false otherwise
     */
    public void setHandled(boolean handled)
    {
        this.handled = handled;
    }

    /**
     * Signals if exception has been handled or not
     * 
     * @return true if exception has been handled, false otherwise
     */
    public boolean handled()
    {
        return handled;
    }

    /**
     * @return MessageProcessor that causes the failure
     */
    public MessageProcessor getFailingMessageProcessor()
    {
        return failingMessageProcessor;
    }

    protected void extractMuleMessage(MuleEvent event)
    {
        this.muleMessage = event == null || VoidMuleEvent.getInstance().equals(event)
                                                                                     ? null
                                                                                     : event.getMessage();
    }

    private void writeObject(ObjectOutputStream out) throws Exception
    {
        out.defaultWriteObject();
        if (this.failingMessageProcessor instanceof Serializable)
        {
            out.writeBoolean(true);
            out.writeObject(this.failingMessageProcessor);
        }
        else
        {
            out.writeBoolean(false);
        }
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();

        boolean failingMessageProcessorWasSerialized = in.readBoolean();
        if (failingMessageProcessorWasSerialized)
        {
            this.failingMessageProcessor = (MessageProcessor) in.readObject();
        }
    }

}
