/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionFactory;
import org.mule.config.i18n.CoreMessages;
import org.mule.transaction.constraints.ConstraintFilter;
import org.mule.util.ClassUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p/> <code>MuleTransactionConfig</code> defines transaction configuration for a
 * transactional endpoint.
 */
public class MuleTransactionConfig implements TransactionConfig, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleTransactionConfig.class);

    public static final String ACTION_NONE_STRING = "NONE";
    public static final String ACTION_ALWAYS_BEGIN_STRING = "ALWAYS_BEGIN";
    public static final String ACTION_BEGIN_OR_JOIN_STRING = "BEGIN_OR_JOIN";
    public static final String ACTION_ALWAYS_JOIN_STRING = "ALWAYS_JOIN";
    public static final String ACTION_JOIN_IF_POSSIBLE_STRING = "JOIN_IF_POSSIBLE";
    public static final String ACTION_NEVER_STRING = "NEVER";

    private TransactionFactory factory;

    private byte action = ACTION_DEFAULT;

    private ConstraintFilter constraint = null;

    private int timeout;
    
    public void setMuleContext(MuleContext context) {
        this.timeout = context.getConfiguration().getDefaultTransactionTimeout();
    }

    public TransactionFactory getFactory()
    {
        return factory;
    }

    public void setFactory(TransactionFactory factory)
    {
        if (factory == null)
        {
            throw new IllegalArgumentException("Transaction Factory cannot be null");
        }
        this.factory = factory;
    }

    public byte getAction()
    {
        return action;
    }

    public void setAction(byte action)
    {
        this.action = action;

    }

    public void setActionAsString(String action)
    {
        if (ACTION_ALWAYS_BEGIN_STRING.equals(action))
        {
            this.action = ACTION_ALWAYS_BEGIN;
        }
        else if (ACTION_BEGIN_OR_JOIN_STRING.equals(action))
        {
            this.action = ACTION_BEGIN_OR_JOIN;
        }
        else if (ACTION_ALWAYS_JOIN_STRING.equals(action))
        {
            this.action = ACTION_ALWAYS_JOIN;
        }
        else if (ACTION_JOIN_IF_POSSIBLE_STRING.equals(action))
        {
            this.action = ACTION_JOIN_IF_POSSIBLE;
        }
        else if (ACTION_NONE_STRING.equals(action))
        {
            this.action = ACTION_NONE;
        }
        else if (ACTION_NEVER_STRING.equals(action))
        {
            this.action = ACTION_NEVER;
        }
        else
        {
            throw new IllegalArgumentException("Action " + action + " is not recognised as a begin action.");
        }
    }

    public String getActionAsString()
    {
        switch (action)
        {
            case ACTION_ALWAYS_BEGIN:
                return ACTION_ALWAYS_BEGIN_STRING;
            case ACTION_BEGIN_OR_JOIN:
                return ACTION_BEGIN_OR_JOIN_STRING; 
            case ACTION_ALWAYS_JOIN:
                return ACTION_ALWAYS_JOIN_STRING;
            case ACTION_JOIN_IF_POSSIBLE:
                return ACTION_JOIN_IF_POSSIBLE_STRING;
            case ACTION_NONE:
                return ACTION_NONE_STRING;
            default :
                return ACTION_NEVER_STRING;
        }
    }

    public boolean isTransacted()
    {
        Transaction tx = TransactionCoordination.getInstance().getTransaction(); 
        boolean joinPossible = (action != ACTION_JOIN_IF_POSSIBLE || (action == ACTION_JOIN_IF_POSSIBLE && tx != null));
        if (action != ACTION_NEVER && action != ACTION_NONE && factory == null)
        {
            // TODO use TransactionException here? This causes API changes as TE is a checked exception ...
            throw new MuleRuntimeException(CoreMessages.transactionFactoryIsMandatory(getActionAsString()));
        }
        return action != ACTION_NEVER && action != ACTION_NONE && factory.isTransacted() && joinPossible;
    }
    
    public boolean isConfigured()
    {
        return factory != null;
    }

    public ConstraintFilter getConstraint()
    {
        if (constraint == null)
        {
            return null;
        }
        try
        {
            return (ConstraintFilter) constraint.clone();
        }
        catch (CloneNotSupportedException e)
        {
            logger.fatal("Failed to clone ConstraintFilter: " + e.getMessage(), e);
            return constraint;
        }
    }

    public void setConstraint(ConstraintFilter constraint)
    {
        this.constraint = constraint;
    }

    public int getTimeout()
    {
        return timeout;
    }

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("Transaction{factory=")
            .append(factory)
            .append(", action=")
            .append(getActionAsString())
            .append(", timeout=")
            .append(timeout)
            .append("}");
        return buf.toString();
    }
    
    public int hashCode()
    {
        return ClassUtils.hash(new Object[]{factory, new Byte(action), constraint, new Integer(timeout)});
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        final MuleTransactionConfig other = (MuleTransactionConfig) obj;
        return ClassUtils.equal(factory, other.factory)
               && ClassUtils.equal(new Byte(action), new Byte(other.action))
               && ClassUtils.equal(constraint, other.constraint)
               && ClassUtils.equal(new Integer(timeout), new Integer(other.timeout));

    }
    
}
