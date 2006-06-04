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

package org.mule.providers.soap.xfire;

import org.codehaus.xfire.transport.Session;
import org.mule.umo.UMOSession;

/**
 * Mules session wrapper for XFire
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireMuleSession implements Session
{

    UMOSession session;

    public XFireMuleSession(UMOSession session)
    {
        if (session == null) {
            throw new NullPointerException("UMOSession");
        }
        this.session = session;
    }

    /**
     * Get a variable from the session by the key.
     * 
     * @param key
     * @return Value
     */
    public Object get(Object key)
    {
        return session.getProperty(key);
    }

    /**
     * Put a variable into the session with a key.
     * 
     * @param key
     * @param value
     */
    public void put(Object key, Object value)
    {
        session.setProperty(key, value);
    }
}
