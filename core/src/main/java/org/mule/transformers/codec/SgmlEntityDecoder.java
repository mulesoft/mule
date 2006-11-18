/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.codec;


/**
 * Decodes a String containing XML entities into its proper XML representation
 * 
 * @deprecated use {@link XmlEntityDecoder} instead
 */
public class SgmlEntityDecoder extends XmlEntityDecoder
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7958646035249197129L;

    public SgmlEntityDecoder()
    {
        super();
        logger.warn(this.getClass().getName() + " is deprecated; please use "
                    + this.getClass().getSuperclass().getName());
    }

}
