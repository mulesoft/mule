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
package org.mule.components.builder;

import org.mule.umo.UMOMessage;

/**
 * A Strategy Class for Building one message from the invocation results of a chain
 * if endpoints. This is used for invoking different endpoints to obain parts of a larger
 * message.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface MessageBuilder {
    Object buildMessage(UMOMessage request, UMOMessage response) throws MessageBuilderException;
}
