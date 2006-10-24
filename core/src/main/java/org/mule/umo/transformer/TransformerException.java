/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.transformer;

import org.mule.config.i18n.Message;
import org.mule.umo.endpoint.EndpointException;

/**
 * <code>TransformerException</code> is a simple exception that is thrown by
 * transformers.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TransformerException extends EndpointException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2943589828020763649L;

    private transient UMOTransformer transformer;

    /**
     * @param message the exception message
     */
    public TransformerException(Message message, UMOTransformer transformer)
    {
        super(message);
        this.transformer = transformer;
        addInfo("Transformer", transformer.toString());
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransformerException(Message message, UMOTransformer transformer, Throwable cause)
    {
        super(message, cause);
        this.transformer = transformer;
        addInfo("Transformer", transformer.toString());
    }

    public TransformerException(UMOTransformer transformer, Throwable cause)
    {
        super(cause);
        this.transformer = transformer;
        addInfo("Transformer", (transformer == null ? "null" : transformer.toString()));
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

    public UMOTransformer getTransformer()
    {
        return transformer;
    }
}
