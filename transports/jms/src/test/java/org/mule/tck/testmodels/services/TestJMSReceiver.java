/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.services;

import javax.jms.TextMessage;

public class TestJMSReceiver extends TestReceiver
{
    public String receive(TextMessage message) throws Exception
    {
        String answer = "Received: " + message.getText();

        if (logger.isDebugEnabled())
        {
            logger.debug(answer + " Number: " + inc() + " in thread: " + Thread.currentThread().getName());
            logger.debug("Message ID is: " + message.getJMSMessageID());
        }

        return answer;
    }
}
