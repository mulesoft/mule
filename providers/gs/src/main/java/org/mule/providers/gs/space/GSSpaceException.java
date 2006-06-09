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
package org.mule.providers.gs.space;

import org.mule.config.i18n.Message;
import org.mule.umo.space.UMOSpaceException;

/**
 * Is thrown when any caught exception is fired in the GSSpace and GSSpaceFactory.
 *
 * @see GSSpace
 * @see GSSpaceFactory
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GSSpaceException extends UMOSpaceException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3282831931874122222L;

    public GSSpaceException(Throwable cause) {
        super(Message.createStaticMessage(cause.getMessage()), cause);
    }
}
