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
package org.mule.transaction.xa.queue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.doomdark.uuid.UUIDGenerator;
import org.mule.transaction.xa.file.DeleteException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class FilePersistenceStrategy implements QueuePersistenceStrategy {

	private static final Log logger = LogFactory.getLog(FilePersistenceStrategy.class);
	
	public static final String EXTENSION = ".msg";
	
	private File storeDir;
    private UUIDGenerator gen = UUIDGenerator.getInstance();
    private Map objects = new HashMap();
	
	public FilePersistenceStrategy(File storeDir) throws IOException {
		this.storeDir = storeDir.getCanonicalFile();
		this.storeDir.mkdirs();
	}
	
	protected String getFileName(Object obj) {
		String id = gen.generateRandomBasedUUID().toString();
		if (obj instanceof TransactionalQueueManager.Holder) {
			id = ((TransactionalQueueManager.Holder) obj).queueName + "/" + id;
		}
		return id;
	}
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#store(java.lang.Object)
	 */
	public Object store(Object obj) throws IOException {
		String id = getFileName(obj);
        File file = new File(storeDir, id + EXTENSION);
        file.getParentFile().mkdirs();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(obj);
        oos.close();
        objects.put(id, obj);
        return id;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#remove(java.lang.Object)
	 */
	public void remove(Object id) throws IOException {
		objects.remove(id);
		File file = new File(storeDir, id + EXTENSION);
		if (file.exists()) {
			if (!file.delete()) {
				throw new DeleteException(file);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#load(java.lang.Object)
	 */
	public Object load(Object id) throws IOException {
		return objects.get(id);
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#restore()
	 */
	public List restore() throws IOException {
		try {
			ArrayList msgs = new ArrayList();
			restoreFiles(storeDir, msgs);
			return msgs;
		} catch (ClassNotFoundException e) {
			throw (IOException) new IOException("Could not restore").initCause(e);
		}
	}
	
	protected void restoreFiles(File dir, List msgs) throws IOException, ClassNotFoundException {
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				restoreFiles(files[i], msgs);
			} else if (files[i].getName().endsWith(EXTENSION)) {
				FileInputStream fis = null;
				try {
	                fis = new FileInputStream(files[i]);
	                ObjectInputStream ois = null;
	                try {
		                ois = new ObjectInputStream(fis);
		                Object object = ois.readObject();
						String name = files[i].getCanonicalPath();
						name = name.substring(storeDir.getCanonicalPath().length() + 1, name.length() - EXTENSION.length());
		                objects.put(name, object);
		                msgs.add(name);
		                logger.debug("File entry: " + name);
	                } finally {
	                	if (ois != null) {
	                		ois.close();
	                	}
	                }
				} finally {
					if (fis != null) {
						fis.close();
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#close()
	 */
	public void close() throws IOException {
		// Nothing to do
	}

}
