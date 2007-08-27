/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.transformer;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * TODO
 */

public interface UMOStreamingTransformer extends UMOBaseTransformer
{

    /**
     * Transformers can be chained together and invoked in a series
     * 
     * @return the next transformer to invoke after this one
     */
    UMOStreamingTransformer getNextTransformer();

    /**
     * Transformers can be chained together and invoked in a series
     * 
     * @param nextTransformer the next transforer to invoke
     */
    void setNextTransformer(UMOStreamingTransformer nextTransformer);

    /**
     * Thransforms the supllied data and returns the result
     * 
     * @param src the inputStream
     * @param encoding the encoding to use in this transformation (if necessary)
     * @return the transformed data
     * @throws TransformerException if a error occurs transforming the data or if the
     *             expected returnClass isn't the same as the transformed data
     */
    // TODO RM: shouldn't this method be void since the "result" is in OutputStream?
    Object transform(InputStream src, OutputStream dest, String encoding) throws TransformerException;

}
