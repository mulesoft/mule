/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
