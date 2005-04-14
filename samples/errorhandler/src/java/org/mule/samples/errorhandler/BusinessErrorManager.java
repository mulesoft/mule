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
 *
 */
 
package org.mule.samples.errorhandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.StringMessageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 *  <code>BusinessErrorManager</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class BusinessErrorManager implements Callable
{
    /** logger used by this class */
    private static transient Log logger = LogFactory.getLog(BusinessErrorManager.class);
    
    /* (non-Javadoc)
     * @see org.mule.umo.lifecycle.AsynchronousCallable#onEvent(org.mule.umo.UMOEvent)
     */
    public Object onCall(UMOEventContext context) throws UMOException
    {
        ErrorMessage msg = (ErrorMessage)context.getTransformedMessage();
        //Do something with the error message
        List msgs = new ArrayList();
        msgs.add("Received Error Message in the Sample Business Error Manager.");
        msgs.add("Error is: " + msg.getException().getDetailMessage());
        msgs.add("Error class: " + msg.getException().getClass().getName()); 
           
        logger.info("\n" + StringMessageHelper.getBoilerPlate(msgs, '*', 80));
        context.setStopFurtherProcessing(true);
        return null;
    }

}
