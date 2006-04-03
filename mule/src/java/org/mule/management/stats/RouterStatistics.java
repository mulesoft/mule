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
package org.mule.management.stats;

import org.mule.management.stats.printers.SimplePrinter;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>RouterStatistics</code> todo
 * 
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @version $Revision$
 */
public class RouterStatistics implements Statistics
{

    public static final int TYPE_INBOUND = 1;
    public static final int TYPE_OUTBOUND = 2;
    public static final int TYPE_RESPONSE = 3;

    private boolean enabled;
    private long notRouted;
    private long caughtInCatchAll;
    private long totalRouted;
    private long totalReceived;
    private Map routed;
    private int type;

    /**
     * @see org.mule.management.stats.Statistics#clear()
     */
    public synchronized void clear()
    {
        notRouted = 0;
        totalRouted = 0;
        totalReceived = 0;
        caughtInCatchAll = 0;
        routed.clear();
    }

    /**
     * @see org.mule.management.stats.Statistics#isEnabled()
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public void logSummary()
    {
        logSummary(new SimplePrinter(System.out));
    }

    public void logSummary(PrintWriter printer)
    {
        printer.print(this);
    }

    /**
     * @see org.mule.management.stats.Statistics#setEnabled(boolean)
     */
    public synchronized void setEnabled(boolean b)
    {
        enabled = b;
    }

    /**
     * The constructor
     */
    public RouterStatistics(int type)
    {
        super();
        this.type = type;
        routed = new HashMap();
    }

    /**
     * Increment routed message for multiple endpoints
     * 
     * @param endpoints The endpoint collection
     */
    public void incrementRoutedMessage(Collection endpoints)
    {
        if (endpoints == null || endpoints.isEmpty()) {
            return;
        }
        List list = new ArrayList(endpoints);
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                incrementRoutedMessage((UMOEndpoint) list.get(i));
            }
        }
    }

    /**
     * Increment routed message for an endpoint
     * 
     * @param endpoint The endpoint
     */
    public synchronized void incrementRoutedMessage(UMOImmutableEndpoint endpoint)
    {
        if (endpoint == null) {
            return;
        }

        String name = endpoint.getName();

        Long cpt = (Long) routed.get(name);
        long count = 0;

        if (cpt != null) {
            count = cpt.longValue();
        }

        // TODO we should probably use a MutableLong here,
        // but that might be problematic for remote MBean access (serialization)
        routed.put(name, new Long(++count));

        totalRouted++;
        totalReceived++;
    }

    /**
     * Increment no routed message
     */
    public synchronized void incrementNoRoutedMessage()
    {
        notRouted++;
        totalReceived++;
    }

    /**
     * Increment no routed message
     */
    public synchronized void incrementCaughtMessage()
    {
        caughtInCatchAll++;
    }

    /**
     * @return Returns the notRouted.
     */
    public final long getCaughtMessages()
    {
        return caughtInCatchAll;
    }

    /**
     * @return Returns the notRouted.
     */
    public final long getNotRouted()
    {
        return notRouted;
    }

    /**
     * @return Returns the totalReceived.
     */
    public final long getTotalReceived()
    {
        return totalReceived;
    }

    /**
     * @return Returns the totalRouted.
     */
    public final long getTotalRouted()
    {
        return totalRouted;
    }

    /**
     * @return Returns the totalRouted.
     */
    public final long getRouted(String endpointName)
    {
        Long l = (Long) routed.get(endpointName);

        if (l == null) {
            return 0;
        } else {
            return l.longValue();
        }
    }

    public boolean isInbound()
    {
        return type == TYPE_INBOUND;
    }

    public Map getRouted()
    {
        return routed;
    }
}
