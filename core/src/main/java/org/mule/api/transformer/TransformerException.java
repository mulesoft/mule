/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transformer;

import org.mule.api.endpoint.EndpointException;
import org.mule.config.i18n.Message;
import org.mule.transformer.TransformerUtils;

import java.util.List;

/**
 * <code>TransformerException</code> is a simple exception that is thrown by
 * transformers.
 */

public class TransformerException extends EndpointException
{
    private static final String TRANSFORMER = "Transformer";
    
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2943589828020763649L;

    private transient Transformer transformer;

    /**
     * @param message the exception message
     */
    public TransformerException(Message message, Transformer transformer)
    {
        super(message);
        this.transformer = transformer;
        addInfo(TRANSFORMER, transformer.toString());
    }

    public TransformerException(Message message, List<Transformer> transformers)
    {
        super(message);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransformerException(Message message, Transformer transformer, Throwable cause)
    {
        super(message, cause);
        this.transformer = transformer;
        addInfo(TRANSFORMER, transformer.toString());
    }

    public TransformerException(Message message, List<Transformer> transformers, Throwable cause)
    {
        super(message, cause);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

    public TransformerException(Transformer transformer, Throwable cause)
    {
        super(cause);
        this.transformer = transformer;
        addInfo(TRANSFORMER, (transformer == null ? "null" : transformer.toString()));
    }

     public TransformerException(List<Transformer> transformers, Throwable cause)
    {
        super(cause);
        this.transformer = TransformerUtils.firstOrNull(transformers);
        addInfo(TRANSFORMER, TransformerUtils.toString(transformers));
    }

   /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransformerException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * @param message the exception message
     */
    public TransformerException(Message message)
    {
        super(message);
    }

    public Transformer getTransformer()
    {
        return transformer;
    }
}
