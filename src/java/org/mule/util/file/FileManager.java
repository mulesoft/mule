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
package org.mule.util.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.ResourceManagerException;

import javax.transaction.xa.XAResource;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FileManager extends AbstractXAResourceManager {

    private static Log logger = LogFactory.getLog(FileManager.class);

    public synchronized FileSession createSession() {
        return new TransactedFileSession(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#getLogger()
     */
    protected Log getLogger() {
        return logger;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#createTransactionContext(java.lang.Object)
     */
    protected AbstractTransactionContext createTransactionContext(Object session) {
        return new FileTransactionContext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doBegin(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected void doBegin(AbstractTransactionContext context) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doPrepare(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected int doPrepare(AbstractTransactionContext context) {
        return XAResource.XA_OK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doCommit(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doRollback(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException {
        // TODO Auto-generated method stub

    }

    protected class FileTransactionContext extends AbstractTransactionContext {

    }
}
