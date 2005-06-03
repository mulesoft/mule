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
 */
package org.mule.extras.client;

import java.lang.reflect.Method;

import org.mule.config.MuleProperties;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>AbstractEventTransformer</code> adds support for adding method
 * details to the result message.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractEventTransformer extends AbstractTransformer
{
    protected AbstractEventTransformer()
    {
        setReturnClass(UMOMessage.class);
    }

    public UMOMessage transform(Object src, Method method) throws TransformerException
    {
        UMOMessage message = (UMOMessage) transform(src);
        message.setProperty(MuleProperties.MULE_METHOD_PROPERTY, method.getName());
        return message;
    }
}
