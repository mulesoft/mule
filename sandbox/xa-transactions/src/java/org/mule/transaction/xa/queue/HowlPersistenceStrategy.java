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
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.doomdark.uuid.UUID;
import org.doomdark.uuid.UUIDGenerator;
import org.objectweb.howl.log.Configuration;
import org.objectweb.howl.log.LogException;
import org.objectweb.howl.log.LogRecord;
import org.objectweb.howl.log.Logger;
import org.objectweb.howl.log.ReplayListener;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class HowlPersistenceStrategy implements QueuePersistenceStrategy, Runnable {

	private static final Log logger = LogFactory.getLog(HowlPersistenceStrategy.class);
	
	private Logger journal;
	private Map objects = Collections.synchronizedMap(new HashMap());
    private Map marks = Collections.synchronizedMap(new HashMap());
    private SortedSet pendingMarks = Collections.synchronizedSortedSet(new TreeSet());
    private SortedSet unusedMarks = new TreeSet();
    private UUIDGenerator gen = UUIDGenerator.getInstance();
    private Thread marker;
    
    private static final byte STORE_OBJ_BYTE = 0;
    private static final byte STORE_BYTES_BYTE = 1;
    private static final byte STORE_STRING_BYTE = 2;
    private static final byte DELETE_BYTE = 3;
    
    private static final byte[] STORE_OBJ = new byte[] { STORE_OBJ_BYTE };
    private static final byte[] STORE_BYTES = new byte[] { STORE_BYTES_BYTE };
    private static final byte[] STORE_STRING = new byte[] { STORE_STRING_BYTE };
    private static final byte[] DELETE = new byte[] { DELETE_BYTE };

	public HowlPersistenceStrategy(Properties props) throws IOException {
		try {
			init(new Configuration(props));
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			throw (IOException) new InterruptedIOException().initCause(e);
		} catch (Exception e) {
			throw (IOException) new IOException().initCause(e);
		}
	}
	
	public HowlPersistenceStrategy(Configuration cfg) throws IOException {
		try {
			init(cfg);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			throw (IOException) new InterruptedIOException().initCause(e);
		} catch (Exception e) {
			throw (IOException) new IOException().initCause(e);
		}
	}
	
	protected void init(Configuration cfg) throws Exception {
		journal = new Logger(cfg);
		journal.open();
		marker = new Thread(this, "HowlPersistenceStrategy");
		marker.setDaemon(true);
		marker.start();
		journal.setAutoMark(false);
	}
	
	protected UUID getId(Object obj) {
		return gen.generateTimeBasedUUID();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#store(java.lang.Object)
	 */
	public Object store(Object obj) throws IOException {
        try {
        	byte[][] data;
	        UUID id = getId(obj);
        	if (obj instanceof byte[]) {
        		data = new byte[][] { STORE_BYTES, id.asByteArray(), (byte[]) obj };
        	} else if (obj instanceof String) {
        		data = new byte[][] { STORE_STRING, id.asByteArray(), ((String) obj).getBytes() };
        	} else {
    			ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	        ObjectOutputStream oos = new ObjectOutputStream(baos);
    	        oos.writeObject(obj);
    	        oos.close();
    	        data = new byte[][] { STORE_OBJ, id.asByteArray(), baos.toByteArray() };
        	}
        	Long mark = new Long(journal.put(data, false));
        	objects.put(id, obj);
        	marks.put(id, mark);
        	pendingMarks.add(mark);
        	return id;
        } catch (Exception e) {
        	throw (IOException) new IOException("Could not store object").initCause(e);
        }
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#load(java.lang.Object)
	 */
	public Object load(Object id) throws IOException {
		return objects.get(id);
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#remove(java.lang.Object)
	 */
	public void remove(Object id) throws IOException {
		Object obj = objects.remove(id);
		if (id != null) {
			try {
		        byte[][] data = new byte[][] { DELETE, ((UUID) id).asByteArray() };
	        	Long mark1 = new Long(journal.put(data, false));
	        	Long mark2 = (Long) marks.remove(id);
	        	synchronized (unusedMarks) {
	        		unusedMarks.add(mark1);
	        		if (mark2 != null) {
	        			unusedMarks.add(mark2);
	        		}
	        	}
	        	if (mark2 != null) {
	        		pendingMarks.remove(mark2);
	        	}
			} catch (Exception e) {
	        	throw (IOException) new IOException("Could not remove object").initCause(e);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#restore()
	 */
	public List restore() throws IOException {
		try {
			final List ids = new ArrayList();
			journal.replay(new ReplayListener() {
				public void onRecord(LogRecord record) {
					try {
						if (record.length > 0) {
							byte[][] fields = record.getFields();
							UUID id = new UUID(fields[1]);
							Object obj;
							switch (fields[0][0]) {
							case STORE_BYTES_BYTE:
								obj = fields[2];
					        	objects.put(id, obj);
					        	marks.put(id, new Long(record.key));
					        	pendingMarks.add(new Long(record.key));
					        	ids.add(id);
					        	logger.debug("Store entry: " + id);
								break;
							case STORE_STRING_BYTE:
								obj = new String(fields[2]);
					        	objects.put(id, obj);
					        	marks.put(id, new Long(record.key));
					        	pendingMarks.add(new Long(record.key));
					        	ids.add(id);
					        	logger.debug("Store entry: " + id);
								break;
							case STORE_OBJ_BYTE:
								ByteArrayInputStream bais = new ByteArrayInputStream(fields[2]);
								ObjectInputStream ois = new ObjectInputStream(bais);
								obj = ois.readObject();
					        	objects.put(id, obj);
					        	marks.put(id, new Long(record.key));
					        	pendingMarks.add(new Long(record.key));
					        	ids.add(id);
					        	logger.debug("Store entry: " + id);
								break;
							case DELETE_BYTE:
								obj = objects.remove(id);
								Long mark = (Long) marks.remove(id);
								synchronized (unusedMarks) {
									unusedMarks.add(new Long(record.key));
									if (mark != null) {
										pendingMarks.remove(mark);
										unusedMarks.add(mark);
									}
								}
					        	ids.remove(id);
					        	logger.debug("Delete entry: " + id);
								break;
							default:
								throw new RuntimeException("Record should be a store or delete entry");
							}
						}
					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				public void onError(LogException exception) {
					throw new RuntimeException(exception);
				}
				public LogRecord getLogRecord() {
					return new LogRecord(1024);
				} 
			});
			logger.debug("Restore retrieved " + ids.size() + " objects");
			return ids;
		} catch (Exception e) {
			throw (IOException) new IOException("Could not restore").initCause(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#close()
	 */
	public void close() throws IOException {
		try {
			marker.interrupt();
			journal.close();
		} catch (InterruptedException e) {
			throw (IOException) new InterruptedIOException("Error closing howl journal").initCause(e);
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			while (!Thread.interrupted()) {
				Thread.sleep(1000);
				if (unusedMarks.size() > 0) {
					//logger.debug("Marker thread started");
					Long pendingMark;
					if (pendingMarks.size() > 0) {
						pendingMark = (Long) pendingMarks.first();
					} else {
						pendingMark = new Long(Long.MAX_VALUE);
					}
					synchronized (unusedMarks) {
						SortedSet marks = unusedMarks.headSet(pendingMark);
						if (marks.size() > 0) {
							if (logger.isDebugEnabled()) {
								logger.debug("Marking to " + Long.toHexString(((Long) marks.last()).longValue()));
							}
							try {
								journal.mark(((Long) marks.last()).longValue());
								marks.clear();
							} catch (InterruptedException e) {
								throw e;
							} catch (Exception e) {
								logger.error("Could not mark", e);
							}
						} else {
							logger.debug("No new marks");
						}
					}
					//logger.debug("Marker thread finished");
				}
			}
		} catch (InterruptedException e) {
			logger.debug("Marker thread interrupted");
		}
	}

}
