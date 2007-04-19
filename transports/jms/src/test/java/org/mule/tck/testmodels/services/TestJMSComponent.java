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

public class TestJMSComponent extends TestComponent
{
    public String receiveJms(TextMessage message) throws Exception
    {
        String answer = "Received: " + message.getText();

        logger.info(answer + " Number: " + inc() + " in thread: " + Thread.currentThread().getName());

        return answer;
    }

}
