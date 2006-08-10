/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
 * @version $Revision$
 */
public class HandlerException extends UMOException {

    public HandlerException(String message) {
        super(Message.createStaticMessage(message));
    }

    public HandlerException(String message, Throwable cause) {
        super(Message.createStaticMessage(message), cause);
    }
}
