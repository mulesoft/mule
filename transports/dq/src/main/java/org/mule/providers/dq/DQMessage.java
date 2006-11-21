/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.dq;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>DQMessage</code> is an encapsulation of a DataQueue message.
 */
public class DQMessage implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5819476148531491540L;

    public static final String XML_ROOT = "DQMessage";
    public static final String XML_ENTRY = "entry";
    public static final String XML_NAME = "name";

    private Map entries = new HashMap();
    private String senderInformation;

    /**
     * @return Returns the senderInformation.
     */
    public final String getSenderInformation()
    {
        return senderInformation;
    }

    /**
     * @param pSenderInformation The senderInformation to set.
     */
    public final void setSenderInformation(final String pSenderInformation)
    {
        senderInformation = pSenderInformation;
    }

    /**
     * Constructor
     */
    public DQMessage()
    {
        super();
    }

    /**
     * The constructor
     * 
     * @param pMessage The message
     */
    public DQMessage(final DQMessage pMessage)
    {
        this();
        if (pMessage == null)
        {
            return;
        }
        this.entries = new LinkedHashMap(pMessage.entries);
        this.senderInformation = pMessage.senderInformation;
    }

    /**
     * Add an entry
     * 
     * @param name The name
     * @param value The value
     */
    public final void addEntry(final String name, final Object value)
    {
        entries.put(name, value);
    }

    /**
     * Returns a value entry by name
     * 
     * @param name The name
     * @return The value
     */
    public final Object getEntry(final String name)
    {
        return entries.get(name);
    }

    /**
     * @return The entries
     */
    public final Iterator getEntries()
    {
        return entries.values().iterator();
    }

    /**
     * @return The entry names
     */
    public final List getEntryNames()
    {
        ArrayList list = new ArrayList();

        Iterator it = entries.keySet().iterator();

        while (it.hasNext())
        {
            list.add(it.next());
        }

        return list;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof DQMessage))
        {
            return false;
        }
        final DQMessage dqMessage = (DQMessage)o;
        if (entries != null ? !entries.equals(dqMessage.entries) : dqMessage.entries != null)
        {
            return false;
        }
        if (senderInformation != null
                        ? !senderInformation.equals(dqMessage.senderInformation)
                        : dqMessage.senderInformation != null)
        {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        int result;
        result = (entries != null ? entries.hashCode() : 0);
        result = 29 * result + (senderInformation != null ? senderInformation.hashCode() : 0);
        return result;
    }
}
