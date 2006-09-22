/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
 
package org.mule.samples.errorhandler.handlers;

import org.mule.samples.errorhandler.AbstractExceptionHandler;
import org.mule.samples.errorhandler.ErrorMessage;
import org.mule.samples.errorhandler.HandlerException;
import org.mule.util.StringMessageUtils;

/**
 *  <code>DefaultHandler</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DefaultHandler extends AbstractExceptionHandler {


    public DefaultHandler() {
        super();
        registerException(Throwable.class);
    }

    public void processException(ErrorMessage message, Throwable t) throws HandlerException {

        System.out.println( StringMessageUtils.getBoilerPlate("Exception received in /n" +
                " DEFAULT EXCEPTION HANDLER /n." +
                " Logic could be put in here to enrich the message content"));
    }

}
