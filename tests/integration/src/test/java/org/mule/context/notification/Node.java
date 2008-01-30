/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * We test notifications by defining a "tree" of expected responses (this is needed because the system is
 * multithreaded and only some ordering is guaranteed serial; other actions happen in parallel)
 * Each node can test for a notification and then has a set of parallel nodes, which describe which notifications
 * happen next in any order.
 * Finally, after all parallel nodes are matched, a node has a set of serial nodes, which are matched in order.
 *
 * <p>Note that nested nodes may themselves have structure and that empty nodes are available, which can
 * help group dependencies.
 *
 * <p>More exactly, we specify a tree and a traversal - the traversal is hardcoded below, and implicit in
 * the instructions above.
 */
class Node implements RestrictedNode
{

    // enumeration describing result of checking at this node
    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int EMPTY = 2;

    // the data for this node
    private Class clazz = null;
    private int action;
    private String id;
    private boolean nodeOk = false;

    // any of these can run after this
    private Set parallel = new HashSet();
    // only once the parallel are done, this runs
    private LinkedList serial = new LinkedList();

    public Node(Class clazz, int action, String id)
    {
        this.clazz = clazz;
        this.action = action;
        this.id = id;
    }

    public Node(Class clazz, int action)
    {
        this(clazz, action, null);
    }

    public Node()
    {
        nodeOk = true;
    }

    public Node parallel(RestrictedNode node)
    {
        parallel.add(node);
        return this;
    }

    public RestrictedNode serial(RestrictedNode node)
    {
        serial.addLast(node);
        return this;
    }

    /**
     * @param notification
     * @return whether the notification was matched or not (for this node or any child)
     */
    public int match(ServerNotification notification)
    {
        // if we need to check ourselves, just do that
        if (!nodeOk)
        {
            if (testLocal(notification))
            {
                nodeOk = true;
                return SUCCESS;
            }
            else
            {
                return FAILURE;
            }
        }

        // otherwise, if we have parallel children, try them
        if (parallel.size() > 0)
        {
            for (Iterator children = parallel.iterator(); children.hasNext();)
            {
                Node child = (Node) children.next();
                switch (child.match(notification))
                {
                case SUCCESS:
                    return SUCCESS;
                case EMPTY: // the node was empty, clean out
                    children.remove();
                    break;
                case FAILURE:
                    break;
                default:
                    throw new IllegalStateException("Bad return from child");
                }
            }
        }

        // if we've still got parallel children, we failed
        if (parallel.size() > 0)
        {
            return FAILURE;
        }

        // otherwise, serial children
        if (serial.size() > 0)
        {
            for (Iterator children = serial.iterator(); children.hasNext();)
            {
                Node child = (Node) children.next();
                switch (child.match(notification))
                {
                case SUCCESS:
                    return SUCCESS;
                case EMPTY: // the node was empty, clean out
                    children.remove();
                    break;
                case FAILURE:
                    return FAILURE; // note this is different to parallel case
                default:
                    throw new IllegalStateException("Bad return from child");
                }
            }

        }

        if (serial.size() > 0)
        {
            return FAILURE;
        }
        else
        {
            return EMPTY;
        }
    }

    private boolean testLocal(ServerNotification notification)
    {
        return clazz.equals(notification.getClass())
                && action == notification.getAction()
                && (null == id || id.equals(notification.getResourceIdentifier()));
    }

    public boolean contains(Class clazz, int action)
    {
        if (null != this.clazz && this.clazz.equals(clazz) && this.action == action)
        {
            return true;
        }
        for (Iterator children = parallel.iterator(); children.hasNext();)
        {
            if (((RestrictedNode) children.next()).contains(clazz, action))
            {
                return true;
            }
        }
        for (Iterator children = serial.iterator(); children.hasNext();)
        {
            if (((RestrictedNode) children.next()).contains(clazz, action))
            {
                return true;
            }
        }
        return false;
    }

}
