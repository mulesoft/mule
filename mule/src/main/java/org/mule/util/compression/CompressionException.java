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
package org.mule.util.compression;

import java.io.IOException;

/**
 * <code>CompressionException</code> TODO -document class
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CompressionException extends IOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 8587532237749889185L;

    public CompressionException(String message)
    {
        super(message);
    }

    public CompressionException(String message, Throwable cause)
    {
        super(message);
        initCause(cause);
    }

}
