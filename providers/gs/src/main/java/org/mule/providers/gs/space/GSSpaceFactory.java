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
package org.mule.providers.gs.space;

import com.j_spaces.core.client.FinderException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.space.CreateSpaceException;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;
import org.mule.umo.space.UMOSpaceFactory;

/**
 * Creates a GigiSpaces JavaSpace
 *
 * @see GSSpace
 * @see net.jini.space.JavaSpace
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GSSpaceFactory implements UMOSpaceFactory {

    private boolean enableMonitorEvents = true;

    public UMOSpace create(String spaceIdentifier) throws UMOSpaceException {
        if(spaceIdentifier==null) {
            throw new NullPointerException(new Message(Messages.X_IS_NULL, "spaceIdentifier").toString());
        }
        try {
            return new GSSpace(spaceIdentifier, enableMonitorEvents);
        } catch (FinderException e) {
            throw new CreateSpaceException(e);
        }
    }
}
