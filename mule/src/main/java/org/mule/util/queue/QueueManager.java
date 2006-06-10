/* 
 * $Id$
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
package org.mule.util.queue;

import org.mule.util.xa.ResourceManagerSystemException;

/**
 * A Queue manager is responsible for manageing one or more Queue resources and providing
 * common support fot transactions and persistence
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface QueueManager
{

    void start() throws ResourceManagerSystemException;

    void stop() throws ResourceManagerSystemException;

    QueueSession getQueueSession();

    void close();

    void setDefaultQueueConfiguration(QueueConfiguration config);

    void setQueueConfiguration(String queueName, QueueConfiguration config);

     /**
     * @return Returns the persistenceStrategy.
     */
    public QueuePersistenceStrategy getPersistenceStrategy();
    /**
     * @param persistenceStrategy The persistenceStrategy to set.
     */
    public void setPersistenceStrategy(QueuePersistenceStrategy persistenceStrategy);

    public QueuePersistenceStrategy getMemoryPersistenceStrategy();

    public void setMemoryPersistenceStrategy(QueuePersistenceStrategy memoryPersistenceStrategy);

}
