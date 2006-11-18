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
 * Encodes an XML String into entity-encoded form
 * 
 * @deprecated use {@link XmlEntityEncoder} instead
 */
public class SgmlEntityEncoder extends XmlEntityEncoder
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4229616539951956886L;

    public SgmlEntityEncoder()
    {
        super();
        logger.warn(this.getClass().getName() + " is deprecated; please use "
                    + this.getClass().getSuperclass().getName());
    }

}
