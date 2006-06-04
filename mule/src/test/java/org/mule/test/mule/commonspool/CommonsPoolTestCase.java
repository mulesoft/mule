/*
 * $Id:
 * /cvsroot/mule/mule/src/test/org/mule/test/mule/commonspool/CommonsPoolTestCase.java,v
 * 15:32:40 $
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.test.mule.commonspool;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.mule.config.pool.CommonsPoolFactory;
import org.mule.config.pool.CommonsPoolProxyPool;
import org.mule.impl.MuleDescriptor;
import org.mule.tck.model.AbstractPoolTestCase;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOPoolFactory;
import org.mule.util.ObjectPool;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CommonsPoolTestCase extends AbstractPoolTestCase
{

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.test.mule.AbstractPoolTestCase#createPool(org.mule.umo.UMODescriptor,
     *      byte)
     */
    public ObjectPool createPool(MuleDescriptor descriptor, byte action) throws InitialisationException
    {
        GenericObjectPool.Config config = new GenericObjectPool.Config();
        config.maxActive = DEFAULT_POOL_SIZE;
        config.maxWait = DEFAULT_WAIT;

        if (action == FAIL_WHEN_EXHAUSTED) {
            config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
        } else if (action == GROW_WHEN_EXHAUSTED) {
            config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_GROW;
        } else if (action == BLOCK_WHEN_EXHAUSTED) {
            config.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;
        } else {
            fail("Action type for pool not recognised. Type is: " + action);
        }
        return new CommonsPoolProxyPool(descriptor, config);
    }

    public UMOPoolFactory getPoolFactory()
    {
        return new CommonsPoolFactory();
    }

}
