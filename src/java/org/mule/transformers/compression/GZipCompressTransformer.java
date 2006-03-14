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

package org.mule.transformers.compression;

import java.io.IOException;

import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.compression.GZipCompression;

/**
 * <code>GZipCompressTransformer</code> is a transformer compressing objects into byte arrays
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GZipCompressTransformer extends AbstractCompressionTransformer
{

	private SerializableToByteArray serializableToByteArray;
	
    public GZipCompressTransformer()
    {
        super();
        this.setStrategy(new GZipCompression());
        this.registerSourceType(Object.class);
        this.setReturnClass(byte[].class);
        serializableToByteArray = new SerializableToByteArray();
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try {
        	byte[] data = (byte[])serializableToByteArray.doTransform(src, encoding);
            return this.getStrategy().compressByteArray(data);
        }
        catch (IOException ioex) {
            throw new TransformerException(this, ioex);
        }
    }

}
