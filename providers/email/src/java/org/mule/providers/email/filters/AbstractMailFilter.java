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
package org.mule.providers.email.filters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOFilter;

import javax.mail.Message;

/**
 * <code>AbstractMailFilter</code> is a base class for all javax.mail.Message
 * filters.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractMailFilter implements UMOFilter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public final boolean accept(Object object)
    {
        if(object==null) throw new NullPointerException("object cannot be null for this filter");
        if(object instanceof Message) {
            return accept((Message)object);
        } else {
            throw new IllegalArgumentException("The Mail filter does not understand: " + object.getClass().getName());
        }
    }

    public abstract boolean accept(Message message);
}
