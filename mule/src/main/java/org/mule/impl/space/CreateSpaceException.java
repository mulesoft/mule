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
package org.mule.impl.space;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.space.UMOSpaceException;

/**
 * Is thrown if an exception is thrown duringthe creation of a space.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CreateSpaceException extends UMOSpaceException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6138791159382128699L;

    public CreateSpaceException(Throwable cause)
    {
        super(new Message(Messages.SPACE_FAILED_TO_CREATE), cause);
    }
}
