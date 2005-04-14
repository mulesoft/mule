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
package org.mule.transaction.xa.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transaction.xa.AbstractTransactionContext;
import org.mule.transaction.xa.AbstractXAResourceManager;
import org.mule.transaction.xa.ResourceManagerException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FileManager extends AbstractXAResourceManager {

	private static Log logger = LogFactory.getLog(FileManager.class);
	
	public synchronized FileSession createSession() {
		return new FileSessionImpl();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#getLogger()
	 */
	protected Log getLogger() {
		return logger;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#createTransactionContext(java.lang.Object)
	 */
	protected AbstractTransactionContext createTransactionContext(Object session) {
		return new FileTransactionContext();
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doBegin(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected void doBegin(AbstractTransactionContext context) {
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doPrepare(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected int doPrepare(AbstractTransactionContext context) {
		return XAResource.XA_OK;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doCommit(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doRollback(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException {
		// TODO Auto-generated method stub
		
	}
	
	protected class FileSessionImpl extends AbstractSession implements FileSession {

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#openInputStream(java.io.File)
		 */
		public FileInputStream openInputStream(File f) throws IOException {
			if (localContext != null) {
				// TODO
				return null;
			} else {
				return new FileInputStream(f);
			}
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#openOutputStream(java.io.File, boolean)
		 */
		public FileOutputStream openOutputStream(File f, boolean append) throws IOException {
			if (localContext != null) {
				// TODO
				return null;
			} else {
				return new FileOutputStream(f, append);
			}
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#openOutputStream(java.io.File)
		 */
		public FileOutputStream openOutputStream(File f) throws IOException {
			return openOutputStream(f, false);
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#mkdir(java.io.File)
		 */
		public boolean mkdir(File f) throws IOException {
			if (localContext != null) {
				// TODO
				return false;
			} else {
				return f.mkdir();
			}
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#openRandomAccess(java.io.File, java.lang.String)
		 */
		public RandomAccessFile openRandomAccess(File f, String mode) throws IOException {
			if (localContext != null) {
				// TODO
				return null;
			} else {
				return new RandomAccessFile(f, mode);
			}
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#delete(java.io.File)
		 */
		public void delete(File f) throws IOException {
			if (localContext != null) {
				// TODO
			} else {
				if (!f.delete()) {
					throw new DeleteException(f);
				}
			}
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#copy(java.io.File, java.io.File)
		 */
		public void copy(File source, File dest) throws IOException {
			if (dest.exists()) {
				delete(dest);
			}
			InputStream is = null;
			OutputStream os = null;
			try {
				is = openInputStream(source);
				try {
					os = openOutputStream(dest);
			        byte[] buffer = new byte[1024 * 4];
			        int n = 0;
			        while (-1 != (n = is.read(buffer))) {
			        	os.write(buffer, 0, n);
			        }
				} finally {
					os.close();
				}
			} finally {
		        is.close();
			}
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.file.FileSession#rename(java.io.File, java.io.File)
		 */
		public void rename(File source, File dest) throws IOException {
			copy(source, dest);
			delete(dest);
		}
		
	}
	
	protected class FileTransactionContext extends AbstractTransactionContext {
		
	}

}
