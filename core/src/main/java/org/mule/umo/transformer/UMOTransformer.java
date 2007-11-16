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

import java.util.List;

/**
 * <code>UMOTransformer</code> can be chained together to covert message payloads
 * from one object type to another.
 */
public interface UMOTransformer extends UMOBaseTransformer
{

    /**
     * Determines if a particular source class can be handled by this transformer
     *
     * @param aClass The class to check for compatability
     * @return true if the transformer supports this type of class or false
     *         otherwise
     */
    boolean isSourceTypeSupported(Class aClass);


    /**
     * Returns an unmodifiable list of Source types registered on this transformer
     *
     * @return an unmodifiable list of Source types registered on this transformer
     */
    List getSourceTypes();

    /**
     * Does this transformer allow null input?
     *
     * @return true if this transformer can accept null input
     */
    boolean isAcceptNull();

    public boolean isIgnoreBadInput();

    /**
     * Thransforms the supplied data and returns the result
     *
     * @param src the data to transform
     * @return the transformed data
     * @throws TransformerException if a error occurs transforming the data or if the
     *                              expected returnClass isn't the same as the transformed data
     */
    Object transform(Object src) throws TransformerException;

    /**
     * Sets the expected return type for the transformed data. If the transformed
     * data is not of this class type a <code>TransformerException</code> will be
     * thrown.
     *
     * @param theClass the expected return type class
     */
    void setReturnClass(Class theClass);

    /** @return the exceptedreturn type */
    Class getReturnClass();

}
