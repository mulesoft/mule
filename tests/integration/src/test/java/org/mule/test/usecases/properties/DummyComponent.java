/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.properties;

import java.util.ArrayList;
import java.util.List;

import org.mule.impl.RequestContext;

public class DummyComponent {

     public void processData(String theData) {
         System.out.println(theData);
         List recipients = new ArrayList();
         recipients.add("ross.mason@symphonysoft.com");
         recipients.add("ross@rossmason.com");
         RequestContext.getEventContext().getMessage().setProperty("recipients", recipients);
     }
}
