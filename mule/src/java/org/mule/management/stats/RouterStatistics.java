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

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.mule.management.stats.printers.SimplePrinter;
import org.mule.umo.endpoint.UMOEndpoint;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <code>RouterStatistics</code> todo
 *
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @version $Revision$
 */
public class RouterStatistics implements Statistics {

    public static final int TYPE_INBOUND = 1;
    public static final int TYPE_OUTBOUND = 2;
    public static final int TYPE_RESPONSE = 3;

    private boolean enabled;
    private int notRouted;
    private int caughtInCatchAll;
    private int totalRouted;
    private int totalReceived;
    private Map routed;
    private int type;

    /**
     * @see org.mule.management.stats.Statistics#clear()
     */
    public synchronized void clear() {
        notRouted = 0;
        totalRouted = 0;
        totalReceived = 0;
        caughtInCatchAll = 0;
        routed.clear();

    }

    /**
     * @see org.mule.management.stats.Statistics#isEnabled()
     */
    public boolean isEnabled() {

        return enabled;
    }

    public void logSummary() {
        logSummary(new SimplePrinter(System.out));
    }

    public void logSummary(PrintWriter printer) {
        printer.print(this);
    }

    /**
     * @see org.mule.management.stats.Statistics#setEnabled(boolean)
     */
    public synchronized void setEnabled(boolean b) {
        enabled = b;

    }

    /**
     * The constructor
     */
    public RouterStatistics(int type) {
        super();
        this.type=type;
        routed = new ConcurrentHashMap();
    }

    /**
     * Increment routed message for multiple endpoints
     *
     * @param endpoints
     *            The endpoint collection
     */
    public void incrementRoutedMessage(Collection endpoints) {
        if (endpoints == null || endpoints.isEmpty())
            return;

        List list = new ArrayList(endpoints);
        synchronized(list) {
            for (int i = 0; i < list.size(); i++)
            {
                incrementRoutedMessage((UMOEndpoint)list.get(i));
            }
        }
    }

    /**
     * Increment routed message for an endpoint
     *
     * @param endpoint
     *            The endpoint
     */
    public synchronized void incrementRoutedMessage(UMOEndpoint endpoint) {

        if (endpoint == null)
            return;

        String name = endpoint.getName();
        Integer cpt = (Integer) routed.get(name);
        int count = 0;

        if (cpt != null)
            count = cpt.intValue();

        count++;

        routed.put(name, new Integer(count));
        totalRouted++;
        totalReceived++;
    }

    /**
     * Increment no routed message
     */
    public synchronized void incrementNoRoutedMessage() {
        notRouted++;
        totalReceived++;
    }

    /**
     * Increment no routed message
     */
    public synchronized void incrementCaughtMessage() {
        caughtInCatchAll++;
    }

    /**
     * @return Returns the notRouted.
     */
    public final int getCaughtMessages() {
        return caughtInCatchAll;
    }
    
    /**
     * @return Returns the notRouted.
     */
    public final int getNotRouted() {
        return notRouted;
    }
    /**
     * @return Returns the totalReceived.
     */
    public final int getTotalReceived() {
        return totalReceived;
    }
    /**
     * @return Returns the totalRouted.
     */
    public final int getTotalRouted() {
        return totalRouted;
    }
    /**
     * @return Returns the totalRouted.
     */
    public final int getRouted(String endpointName) {
        Integer i = (Integer) routed.get(endpointName);

         if(i==null)
              return 0;

         else
           return i.intValue();
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

