/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.mule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TestExceptionStrategy extends DefaultExceptionStrategy
{

    /**
     * logger used by this class
     */
    private static transient Log logger =
            LogFactory.getLog(TestExceptionStrategy.class);

    private UMOLifecycleAdapter parent;

    public TestExceptionStrategy()
    {
    }

    /* (non-Javadoc)
     * @see org.mule.exception.UMOExceptionStrategy#getComponent()
     */
    public UMOLifecycleAdapter getParent()
    {
        return parent;
    }

    /* (non-Javadoc)
     * @see org.mule.exception.UMOExceptionStrategy#handleException(java.lang.Object, java.lang.Throwable)
     */
    public void handleException(Object message, Throwable t)
    {
        logger.info("*** Logging test exceptin in test Exception Strategy");
        logger.info("*** Message is: " + message);
        logger.info("*** Exception is: " + t, t);

    }

    /* (non-Javadoc)
     * @see org.mule.exception.UMOExceptionStrategy#setComponent(org.mule.impl.UniversalMessageObject)
     */
    public void setParent(UMOLifecycleAdapter parent)
    {
        this.parent = parent;

    }

}
