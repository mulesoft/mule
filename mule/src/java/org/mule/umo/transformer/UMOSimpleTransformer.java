/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.umo.transformer;

import org.mule.umo.lifecycle.Initialisable;

import java.io.Serializable;

/**
 * <code>UMOSimpleTransformer</code> manages the transformation and or
 * translation of one type of data to the other. Source data is received, then
 * processed and returned via the <code>transform()</code> method. <p/> The
 * return Class is specifed so that the return message is validated defore it is
 * returned.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOSimpleTransformer extends Initialisable, Serializable, Cloneable
{
    /**
     * JDK1.3+ 'Service Provider' specification (
     * http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html )
     */
    String SERVICE_ID = "META-INF/services/org.mule.umo.transformer.UMOTransformer";

    /**
     * Thransforms the supllied data and returns the result
     *
     * @param src the data to transform
     * @return the transformed data
     * @throws TransformerException if a error occurs transforming the data or
     *             if the expected returnClass isn't the same as the transformed
     *             data
     */
    Object transform(Object src) throws TransformerException;

    /**
     * @param newName the logical name for the transformer
     */
    void setName(String newName);

    /**
     * @return the logical name of the transformer
     */
    String getName();

    /**
     * Sets the expected return type for the transformed data. If the
     * transformed data is not of this class type a
     * <code>TransformerException</code> will be thrown.
     *
     * @param theClass the expected return type class
     */
    void setReturnClass(Class theClass);

    /**
     * @return the exceptedreturn type
     */
    Class getReturnClass();

    // TODO shouldn't have to declare this but eclipse?? throws an error
    Object clone() throws CloneNotSupportedException;

    UMOTransformer getNextTransformer();

    void setNextTransformer(UMOTransformer nextTransformer);
}
