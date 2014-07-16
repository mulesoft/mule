/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.BlockingServerEvent;

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class Node implements RestrictedNode
{

    // enumeration describing result of checking at this node
    public static final int SUCCESS = 0;
    public static final int FAILURE = 1;
    public static final int EMPTY = 2;

    // the data for this node
    private Class clazz = null;
    private int action;
    private String id;
    private boolean isIdDefined = false; // allow null IDs to be specified
    private boolean nodeOk = false;

    protected final transient Log logger = LogFactory.getLog(this.getClass());

    // any of these can run after this
    private Set parallel = new HashSet();
    // only once the parallel are done, this runs
    private LinkedList serial = new LinkedList();

    public Node(Class clazz, int action, String id)
    {
        this(clazz, action);
        this.id = id;
        isIdDefined = true;
    }

    public Node(Class clazz, int action)
    {
        this.clazz = clazz;
        this.action = action;
    }

    public Node()
    {
        nodeOk = true;
    }

    public Node parallel(RestrictedNode node)
    {
        if (null != node.getNotificationClass() &&
            BlockingServerEvent.class.isAssignableFrom(node.getNotificationClass()))
        {
            logger.warn("Registered blocking event as parallel: " + node);
        }
        parallel.add(node);
        return this;
    }

    /**
     * Avoid warnings when we need to add a synch event as parallel for other reasons
     * (typically because there's more than one model generating some event) 
     */
    public Node parallelSynch(RestrictedNode node)
    {
        if (null != node.getNotificationClass() &&
            !BlockingServerEvent.class.isAssignableFrom(node.getNotificationClass()))
        {
            throw new IllegalStateException("Node " + node + " is not a synch event");
        }
        parallel.add(node);
        return this;
    }

    public RestrictedNode serial(RestrictedNode node)
    {
        if (null != node.getNotificationClass() &&
                !BlockingServerEvent.class.isAssignableFrom(node.getNotificationClass()))
        {
            logger.warn("Registered non-blocking event as serial: " + node);
        }
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
                && (!isIdDefined ||
                (null == id && null == notification.getResourceIdentifier()) ||
                (null != id && id.equals(notification.getResourceIdentifier())));
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

    public RestrictedNode getAnyRemaining()
    {
        if (! nodeOk)
        {
            return this;
        }
        for (Iterator children = parallel.iterator(); children.hasNext();)
        {
            RestrictedNode any = ((RestrictedNode) children.next()).getAnyRemaining();
            if (null != any)
            {
                return any;
            }
        }
        for (Iterator children = serial.iterator(); children.hasNext();)
        {
            RestrictedNode any = ((RestrictedNode) children.next()).getAnyRemaining();
            if (null != any)
            {
                return any;
            }
        }
        return null;
    }

    public boolean isExhausted()
    {
        return null == getAnyRemaining();
    }

    public Class getNotificationClass()
    {
        return clazz;
    }

    public String toString()
    {
        return clazz + ": " + action + (isIdDefined ? ": " + id : "");
    }

}
