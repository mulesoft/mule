/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 14-apr-2005
 *
 */
package org.mule.extras.pgp;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

/**
 * @author ariva
 * 
 */
public class EchoMsg implements Callable
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Callable#onCall(org.mule.umo.UMOEventContext)
     */
    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        return eventContext.getMessageAsString();
    }

}
