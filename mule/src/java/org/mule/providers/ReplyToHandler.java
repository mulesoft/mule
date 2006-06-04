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

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.UMOTransformer;

/**
 * <code>ReplyToHandler</code> is used to handle routing where a replyTo
 * endpointUri is set on the message
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface ReplyToHandler
{
    void processReplyTo(UMOEvent event, UMOMessage returnMessage, Object replyTo) throws UMOException;

    void setTransformer(UMOTransformer transformer);

    UMOTransformer getTransformer();
}
