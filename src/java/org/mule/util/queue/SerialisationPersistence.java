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
 */
package org.mule.util.queue;

import EDU.oswego.cs.dl.util.concurrent.BoundedChannel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEvent;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.queue.SerialisationPersistence.EventFilenameFilter;

import java.io.*;

/**
 * <code>SerialisationPersistence</code> persists event objects to disk.
 * Requires that the event payload is serializable.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SerialisationPersistence implements PersistenceStrategy
{
    public static final String DEFAULT_QUEUE_STORE = MuleManager.getConfiguration().getWorkingDirectoy() + "/queuestore";
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(SerialisationPersistence.class);

    private File store;
    private Object lock = new Object();

    private String queueStore = DEFAULT_QUEUE_STORE;

    public void store(UMOEvent event) throws PersistentQueueException
    {
        synchronized (lock)
        {
            File item = new File(store, event.getEndpoint().getEndpointURI().getAddress() + "/" + event.getId() + ".e");

//        if(item.exists()) {
//            throw new PersistentQueueException("Event: " + event.getId() + " has already been stored at: " + item.getAbsolutePath());
//        }
            try
            {
                EventHolder eventHolder = new EventHolder(event);

                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(item));
                oos.writeObject(eventHolder);
                oos.close();

            } catch (IOException e)
            {
                throw new PersistentQueueException(new Message(Messages.FAILED_TO_PERSIST_EVENT_X, event.getId()), e);
            }
        }
    }

    public boolean remove(UMOEvent event) throws PersistentQueueException
    {
        File item = new File(store, event.getComponent().getDescriptor().getName() + "/" + event.getId() + ".e");
        if (item.exists())
        {
            synchronized (lock)
            {
                return item.delete();
            }
        }
        return false;
    }

    public synchronized void initialise(BoundedChannel queue, String componentName) throws InitialisationException
    {
        if (queue == null)
        {
            throw new InitialisationException(new Message(Messages.X_IS_NULL, "Queue"), this);
        }
        if (store == null)
        {
            store = new File(getQueueStore());
        }
        File componentStore = new File(store, componentName);
        try
        {
            if (componentStore.exists())
            {
                String[] events = componentStore.list(new EventFilenameFilter());
                for (int i = 0; i < events.length; i++)
                {
                    String event = events[i];
                    File eventFile = new File(componentStore, event);
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(eventFile));
                    EventHolder eventHolder = (EventHolder) ois.readObject();

                    UMOEvent umoEvent = eventHolder.getEvent();

                    queue.put(umoEvent);
                    ois.close();
                }
            } else
            {
                if (!componentStore.mkdirs())
                {
                    throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X, "Queue Store: " + store.getAbsolutePath()), this);
                }
            }
        } catch (InitialisationException e)
        {
            throw e;
        } catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X, "Queue Persistent Store"), e, this);
        }
    }

    public String getQueueStore()
    {
        return queueStore;
    }

    public void setQueueStore(String queueStore)
    {
        this.queueStore = queueStore;
    }

    public static class EventFilenameFilter implements FilenameFilter
    {
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".e");
        }
    }

    public void dispose() throws PersistentQueueException
    {
    }
}
