/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transformer;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;
import org.mule.transformer.TransformerUtils;

import java.util.List;

/**
 * <code>TransformerException</code> is a simple exception that is thrown by
 * transformers.
 */

public class TransformerException extends MessagingException
{
    private static final String TRANSFORMER = "Transformer";
    
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2943589828020763650L;

    private transient Transformer transformer;

    /**
     * @param message the exception message
     * @deprecated use TransformerException(Message, MuleEvent, Transformer)
     */
    @Deprecated
    public TransformerException(Message message, MuleMessage muleMessage, Transformer transformer)
    {
        super(message, muleMessage);
        this.transformer = transformer;
        addInfo(TRANSFORMER, transformer.toString());
    }

    public TransformerException(Message message, MuleEvent event, Transformer transformer)
    {
        super(message, event);
        this.transformer = transformer;
        addInfo(TRANSFORMER, transformer.toString());
    }

    /**
     * @deprecated use TransformerException(Message, MuleEvent, List)
     */
    @Deprecated
    public TransformerException(Message message, MuleMessage muleMessage, List<Transformer> transformers)
    {
        super(message, muleMessage);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    public TransformerException(Message message, MuleEvent event, List<Transformer> transformers)
    {
        super(message, event);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     * @deprecated use TransformerException(Message, MuleEvent, Transformer, Throwable)
     */
    @Deprecated
    public TransformerException(Message message, MuleMessage muleMessage, Transformer transformer, Throwable cause)
    {
        super(message, muleMessage, cause);
        this.transformer = transformer;
        addInfo(TRANSFORMER, transformer.toString());
    }

    public TransformerException(Message message, MuleEvent event, Transformer transformer, Throwable cause)
    {
        super(message, event, cause);
        this.transformer = transformer;
        addInfo(TRANSFORMER, transformer.toString());
    }

    /**
     * @deprecated use TransformerException(Message, MuleEvent, List, Throwable)
     */
    @Deprecated
    public TransformerException(Message message, MuleMessage muleMessage, 
        List<Transformer> transformers, Throwable cause)
    {
        super(message, muleMessage, cause);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    public TransformerException(Message message, MuleEvent muleMessage, 
        List<Transformer> transformers, Throwable cause)
    {
        super(message, muleMessage, cause);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    /**
     * @deprecated use TransformerException(MuleEvent, Transformer, Throwable)
     */
    @Deprecated
    public TransformerException(MuleMessage muleMessage, Transformer transformer, Throwable cause)
    {
        super(muleMessage, cause);
        this.transformer = transformer;
        addInfo(TRANSFORMER, (transformer == null ? "null" : transformer.toString()));
    }

    public TransformerException(MuleEvent event, Transformer transformer, Throwable cause)
    {
        super(event, cause);
        this.transformer = transformer;
        addInfo(TRANSFORMER, (transformer == null ? "null" : transformer.toString()));
    }

    /**
     * @deprecated use 
     */
    @Deprecated
    public TransformerException(MuleMessage muleMessage, List<Transformer> transformers, Throwable cause)
    {
        super(muleMessage, cause);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    public TransformerException(MuleEvent event, List<Transformer> transformers, Throwable cause)
    {
        super(event, cause);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     * @deprecated use TransformerException(Message, MuleEvent, Throwable)
     */
    @Deprecated
    public TransformerException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    public TransformerException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }

    /**
     * @param message the exception message
     * @deprecated use TransformerException(Message, MuleEvent)
     */
    @Deprecated
    public TransformerException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public TransformerException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    /**
     * @deprecated Use the constructor with MuleEvent instead
     */
    @Deprecated
    public TransformerException(Message message, Transformer transformer, Throwable cause)
    {
        this(message, (MuleEvent) null, transformer, cause);
    }

    /**
     * @deprecated Use the constructor with MuleEvent instead
     */
    @Deprecated
    public TransformerException(Message message, Transformer transformer)
    {
        this(message, (MuleEvent) null, transformer);
    }

    /**
     * @deprecated Use the constructor with MuleEvent instead
     */
    @Deprecated
    public TransformerException(Transformer transformer, Throwable cause)
    {
        this((MuleEvent) null, transformer, cause);
    }

    /**
     * @deprecated Use the constructor with MuleEvent instead
     */
    @Deprecated
    public TransformerException(Message message, Throwable cause)
    {
        this(message, (MuleEvent) null, cause);
    }
    
    /**
     * @deprecated Use the constructor with MuleEvent instead
     */
    @Deprecated
    public TransformerException(Message message)
    {
        this(message, (MuleEvent) null);
    }
    
    public Transformer getTransformer()
    {
        return transformer;
    }
}
