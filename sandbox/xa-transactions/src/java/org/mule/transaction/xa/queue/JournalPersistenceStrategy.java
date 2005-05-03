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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.activeio.Packet;
import org.activeio.journal.Journal;
import org.activeio.journal.RecordLocation;
import org.activeio.journal.active.JournalImpl;
import org.activeio.journal.active.LogFileManager;
import org.activeio.packet.ByteArrayPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.doomdark.uuid.UUID;
import org.doomdark.uuid.UUIDGenerator;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JournalPersistenceStrategy implements QueuePersistenceStrategy, Runnable {

	private static final Log logger = LogFactory.getLog(JournalPersistenceStrategy.class);
	
    private UUIDGenerator gen = UUIDGenerator.getInstance();
	private Journal journal;
    private Map objects;
    private Map marks;
    private Thread marker;
    private SortedSet pendingMarks;
    private SortedSet unusedMarks;
    private Object markLock = new Object();
	
    private static final byte STORE_OBJ_BYTE = 0;
    private static final byte STORE_BYTES_BYTE = 1;
    private static final byte STORE_STRING_BYTE = 2;
    private static final byte DELETE_BYTE = 3;
    
    private static final Object ACTIVE_MARK = new Object();
    private static final Object INACTIVE_MARK = new Object();
    
	public JournalPersistenceStrategy(File store) throws IOException {
		journal = new JournalImpl(store, 4, LogFileManager.DEFAULT_LOGFILE_SIZE);
		objects = new ConcurrentHashMap();
		marks = new ConcurrentHashMap();
	    pendingMarks = new TreeSet();
	    unusedMarks = new TreeSet();
		marker = new Thread(this, "JournalPersistenceStrategy");
		marker.setDaemon(true);
		marker.start();
	}
	
	protected UUID getId(Object obj) {
		return gen.generateTimeBasedUUID();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#store(java.lang.Object)
	 */
	public Object store(Object obj) throws IOException {
		UUID id = getId(obj);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (obj instanceof byte[]) {
			baos.write(STORE_BYTES_BYTE);
			baos.write(id.asByteArray());
			baos.write((byte[]) obj);
		} else if (obj instanceof String) {
			baos.write(STORE_STRING_BYTE);
			baos.write(id.asByteArray());
			baos.write(((String) obj).getBytes());
		} else {
			baos.write(STORE_OBJ_BYTE);
			baos.write(id.asByteArray());
	        ObjectOutputStream oos = new ObjectOutputStream(baos);
	        oos.writeObject(obj);
	        oos.close();
		}
		Packet packet = new ByteArrayPacket(baos.toByteArray());
		RecordLocation loc = journal.write(packet, false);
		objects.put(id, obj);
		synchronized (markLock) {
			marks.put(id, loc);
			pendingMarks.add(loc);
		}
		return id;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#load(java.lang.Object)
	 */
	public Object load(Object id) throws IOException {
		Object obj = objects.get(id);
		if (obj != null) {
			return obj;
		}
		//
		// The following code read the object from the 
		// journal.  It could be used if a caching
		// strategy is plugged in so that not all objects
		// are kept in memory.
		//
		try {
			RecordLocation loc = (RecordLocation) marks.get(id);
			Packet packet = journal.read(loc);
			return readStorePacket(packet.sliceAsBytes());
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw (IOException) new IOException().initCause(e);
		}
	}
	
	protected Object readStorePacket(byte[] buffer) throws ClassNotFoundException, IOException {
		if (buffer[0] == STORE_BYTES_BYTE) {
			byte[] ba = new byte[buffer.length - 17];
			System.arraycopy(buffer, 17, ba, 0, ba.length);
			return ba;
		} else if (buffer[0] == STORE_STRING_BYTE) {
			String str = new String(buffer, 17, buffer.length - 17);
			return str;
		} else if (buffer[0] == STORE_OBJ_BYTE) {
			ByteArrayInputStream bais = new ByteArrayInputStream(buffer, 17, buffer.length);
			ObjectInputStream ois = new ObjectInputStream(bais);
			Object o = ois.readObject();
			return o;
		} else {
			throw new IllegalStateException("The packet is not a STORE packet.");
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#remove(java.lang.Object)
	 */
	public void remove(Object id) throws IOException {
		objects.remove(id);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(DELETE_BYTE);
		baos.write(((UUID) id).toByteArray());
		Packet packet = new ByteArrayPacket(baos.toByteArray());
		RecordLocation loc = journal.write(packet, false);
		synchronized (markLock) {
			unusedMarks.add(loc);
			loc = (RecordLocation) marks.remove(id);
			if (loc != null) {
				pendingMarks.remove(loc);
				unusedMarks.add(loc);
			}
			markLock.notify();
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#restore()
	 */
	public List restore() throws IOException {
		List results = new ArrayList();
        RecordLocation pos = null;
        logger.info("Journal Recovery Started.");
        try {
    		synchronized (markLock) {
		        // While we have records in the journal.
		        while ((pos = journal.getNextRecordLocation(pos)) != null) {
		            Packet packet = journal.read(pos);
		            byte[] buffer = packet.sliceAsBytes();
		            if (buffer[0] == DELETE_BYTE) {
		            	UUID id = new UUID(buffer, 1);
		            	results.remove(id);
		    			unusedMarks.add(pos);
		    			RecordLocation loc = (RecordLocation) marks.remove(id);
		    			if (loc != null) {
		    				pendingMarks.remove(loc);
		    				unusedMarks.add(loc);
		    			}
		    			markLock.notify();
		            } else {
			            Object obj    = readStorePacket(buffer);
			    		UUID   id     = getId(obj);
		            	results.add(id);
			    		objects.put(id, obj);
		    			marks.put(id, pos);
		    			pendingMarks.add(pos);
		            }
	    		}
	        }
        } catch (IOException e) {
        	throw e;
        } catch (Exception e) {
        	throw (IOException) new IOException().initCause(e);
        }

        logger.info("Journal Recovered: " + results.size() + " message(s) in transactions recovered.");
		return results;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#close()
	 */
	public void close() throws IOException {
		marker.interrupt();
		journal.close();
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		long lastTime = 0;
		try {
			while (!Thread.interrupted()) {
				Thread.sleep(1000);
				if (unusedMarks.isEmpty()) {
					continue;
				}
				synchronized (markLock) {
					// No more pending marks
					// so clear all unused marks and mark to last
					SortedSet marks;
					if (pendingMarks.isEmpty()) {
						marks = unusedMarks;
					// There are pending marks
					} else {
						RecordLocation pendingMark = (RecordLocation) pendingMarks.first();
						marks = unusedMarks.headSet(pendingMark);
					}
					// Is there anything to do ?
					if (!marks.isEmpty()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Marking to " + marks.last());
						}
						try {
							journal.setMark((RecordLocation) marks.last(), false);
							marks.clear();
						} catch (InterruptedIOException e) {
							throw (InterruptedException) new InterruptedException().initCause(e);
						} catch (Exception e) {
							logger.warn("Could not mark", e);
						}
					} else {
						//logger.debug("No new marks");
					}
				}
			}
		} catch (InterruptedException e) {
			logger.debug("Marker thread interrupted");
		}
	}

}

