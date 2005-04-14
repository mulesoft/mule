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
package org.mule.routing.filters;

import org.mule.umo.UMOFilter;
import org.mule.umo.UMOMessage;

/**
 * <code>MessageFilter</code> allows filtering on the whole message not just
 * the payload
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class MessageFilter implements UMOFilter
{
    public final boolean accept(Object object)
    {
        if(object instanceof UMOMessage) {
            return accept((UMOMessage)object);
        }
        return false;
    }

    public abstract boolean accept(UMOMessage message);
}
