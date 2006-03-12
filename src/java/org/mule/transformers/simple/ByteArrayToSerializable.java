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
package org.mule.transformers.simple;

import org.apache.commons.lang.SerializationUtils;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its
 * object representation
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ByteArrayToSerializable extends AbstractTransformer
{
    public ByteArrayToSerializable()
    {
        registerSourceType(byte[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try {
            return SerializationUtils.deserialize((byte[])src);
        }
        catch (Exception e) {
            throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X_TO_X,
                    "byte[]", "Object"), this, e);
        }
    }

}
