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
 */

package org.mule.providers;

import java.io.Serializable;

/**
 * <code>NullPayload</code> represents a null event payload
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class NullPayload implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3406355970240721084L;

    public boolean equals(Object obj)
    {
        return obj instanceof NullPayload;
    }

    public String toString()
    {
        return "{NullPayload}";
    }

}
