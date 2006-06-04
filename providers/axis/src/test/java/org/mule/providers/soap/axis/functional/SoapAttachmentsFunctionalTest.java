/*
 * $Id$ 
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.providers.soap.axis.functional;

import javax.activation.DataHandler;

/**
 * @author <a href="mailto:risears@gmail.com">Rick Sears</a>
 * @version $Revision$
 */
public interface SoapAttachmentsFunctionalTest
{
    public String receiveMessageWithAttachments(String payload, DataHandler[] attachments);
}