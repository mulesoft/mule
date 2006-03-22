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

package org.mule.util.queue;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import org.activeio.Packet;
import org.activeio.journal.Journal;
import org.activeio.journal.JournalEventListener;
import org.activeio.journal.RecordLocation;
import org.activeio.journal.active.JournalImpl;
import org.activeio.journal.active.LogFileManager;
import org.activeio.packet.ByteArrayPacket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.doomdark.uuid.UUID;
import org.doomdark.uuid.UUIDGenerator;
import org.mule.MuleManager;
import org.mule.config.MuleConfiguration;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 
 * TODO: this implementation is in alpha stage defragmentation should be handled
 * correctly
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JournalPersistenceStrategy
        implements
            QueuePersistenceStrategy,
            Runnable,
            JournalEventListener
{

    private static final Log logger = LogFactory.getLog(JournalPersistenceStrategy.class);

    private UUIDGenerator gen = UUIDGenerator.getInstance();
    private Journal journal;
    private Map marks;
    private Thread marker;
    private SortedSet pendingMarks;
    private SortedSet unusedMarks;
    private Object markLock = new Object();
    private RecordLocation overflowLocation = null;
    private File store;

    private static final byte STORE_BYTE = 0;
    private static final byte DELETE_BYTE = 1;

    private static final int UUID_LENGTH = new UUID().asByteArray().length;

    public JournalPersistenceStrategy() throws IOException
    {
        super();
    }

    protected UUID getId(Object obj)
    {
        return gen.generateTimeBasedUUID();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#store(java.lang.Object)
     */
    public Object store(String queue, Object obj) throws IOException
    {
        UUID id = getId(obj);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeByte(STORE_BYTE);
        oos.writeUTF(queue);
        oos.write(id.asByteArray());
        oos.writeObject(obj);
        oos.close();
        Packet packet = new ByteArrayPacket(baos.toByteArray());
        RecordLocation loc = journal.write(packet, false);
        synchronized (markLock) {
            marks.put(id, loc);
            pendingMarks.add(loc);
        }
        return id;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#load(java.lang.Object)
     */
    public Object load(String queue, Object id) throws IOException
    {
        try {
            RecordLocation loc = (RecordLocation)marks.get(id);
            Packet packet = journal.read(loc);
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(packet.sliceAsBytes()));
            ois.readByte();
            ois.readUTF();
            ois.skipBytes(UUID_LENGTH);
            Object obj = ois.readObject();
            return obj;
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw (IOException)new IOException().initCause(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#remove(java.lang.Object)
     */
    public void remove(String queue, Object id) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
        baos.write(DELETE_BYTE);
        baos.write(((UUID)id).asByteArray());
        Packet packet = new ByteArrayPacket(baos.toByteArray());
        RecordLocation loc = journal.write(packet, false);
        synchronized (markLock) {
            unusedMarks.add(loc);
            loc = (RecordLocation)marks.remove(id);
            if (loc != null) {
                pendingMarks.remove(loc);
                unusedMarks.add(loc);
            }
            markLock.notify();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#restore()
     */
    public List restore() throws IOException
    {
        Map results = new HashMap();
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
                        RecordLocation loc = (RecordLocation)marks.remove(id);
                        if (loc != null) {
                            pendingMarks.remove(loc);
                            unusedMarks.add(loc);
                        }
                        markLock.notify();
                    }
                    else {
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer));
                        ois.readByte();
                        String queue = ois.readUTF();
                        byte[] buf = new byte[UUID_LENGTH];
                        ois.read(buf);
                        UUID id = new UUID(buf);
                        results.put(id, new HolderImpl(queue, id));
                        marks.put(id, pos);
                        pendingMarks.add(pos);
                    }
                }
            }
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw (IOException)new IOException().initCause(e);
        }

        logger.info("Journal Recovered: " + results.size() + " message(s) in transactions recovered.");
        return new ArrayList(results.values());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.util.queue.QueuePersistenceStrategy#open()
     */
    public void open() throws IOException
    {
        File s = store;
        if (s == null) {
            s = new File(MuleManager.getConfiguration().getWorkingDirectory() + File.separator
                    + MuleConfiguration.DEFAULT_QUEUE_STORE);
        }
        s.mkdirs();
        journal = new JournalImpl(s, 8, LogFileManager.DEFAULT_LOGFILE_SIZE);
        journal.setJournalEventListener(this);
        marks = new ConcurrentHashMap();
        pendingMarks = new TreeSet();
        unusedMarks = new TreeSet();
        marker = new Thread(this, "JournalPersistenceStrategy");
        marker.setDaemon(true);
        marker.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.queue.QueuePersistenceStrategy#close()
     */
    public void close() throws IOException
    {
        marker.interrupt();
        journal.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        try {
            while (!Thread.interrupted()) {
                Thread.sleep(100);

                synchronized (markLock) {
                    try {
                        SortedSet m;
                        if (overflowLocation != null) {
                            RecordLocation overflowLoc = overflowLocation;
                            overflowLocation = null;
                            m = pendingMarks.headSet(overflowLoc);
                            if (m != null && m.size() > 0) {
                                logger.trace("Relocating " + m.size() + " records");
                                int nbRecord = 0;
                                while (m.size() > 0) {
                                    RecordLocation loc = (RecordLocation)m.first();
                                    Packet packet = journal.read(loc);
                                    RecordLocation newLoc = journal.write(packet, false);
                                    ObjectInputStream ois = new ObjectInputStream(
                                            new ByteArrayInputStream(packet.sliceAsBytes()));
                                    ois.readByte();
                                    String queue = ois.readUTF();
                                    byte[] buf = new byte[UUID_LENGTH];
                                    ois.read(buf);
                                    UUID id = new UUID(buf);

                                    if (++nbRecord % 10 == 0) {
                                        RecordLocation n = journal.getNextRecordLocation(loc);
                                        journal.setMark(n != null ? n : loc, false);
                                        // unusedMarks.headSet(loc).clear();
                                    }
                                    unusedMarks.add(loc);
                                    pendingMarks.add(newLoc);
                                    marks.put(id, newLoc);
                                    m.remove(loc);
                                }
                            }
                        }
                        // No more pending marks
                        // so clear all unused marks and mark to last
                        if (pendingMarks.isEmpty()) {
                            m = unusedMarks;
                            // There are pending marks
                        }
                        else {
                            RecordLocation pendingMark = (RecordLocation)pendingMarks.first();
                            m = unusedMarks.headSet(pendingMark);
                        }
                        // Is there anything to do ?
                        if (!m.isEmpty()) {
                            RecordLocation last = (RecordLocation)m.last();
                            RecordLocation n = journal.getNextRecordLocation((RecordLocation)m.last());
                            if (n == null) {
                                n = last;
                            }
                            if (logger.isDebugEnabled()) {
                                logger.debug("Marking to " + n + " / "
                                        + (pendingMarks.isEmpty() ? "null" : pendingMarks.last()));
                            }
                            if (journal.getMark() == null || n.compareTo(journal.getMark()) > 0) {
                                journal.setMark(n, false);
                            }
                            m.clear();
                        }
                    }
                    catch (Exception e) {
                        logger.warn("Error when relocating records", e);
                    }
                }
            }
        }
        catch (InterruptedException e) {
            logger.debug("Marker thread interrupted");
        }
    }

    protected static class HolderImpl implements Holder
    {
        private String queue;
        private Object id;

        public HolderImpl(String queue, Object id)
        {
            this.queue = queue;
            this.id = id;
        }

        public Object getId()
        {
            return id;
        }

        public String getQueue()
        {
            return queue;
        }
    }

    public void overflowNotification(RecordLocation safeLoc)
    {
        overflowLocation = safeLoc;
        logger.debug("Overflow to " + safeLoc);
    }

    public File getStore()
    {
        return store;
    }

    public void setStore(File store)
    {
        this.store = store;
    }

    public boolean isTransient()
    {
        return false;
    }
}
