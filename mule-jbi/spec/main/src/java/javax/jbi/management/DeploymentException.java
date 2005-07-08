// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * DeploymentException.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi.management;

/**
 * DeploymentException is an exception thrown by the
 * Deployment Service.
 *
 * @author JSR208 Expert Group
 */

public class DeploymentException extends javax.jbi.JBIException
{
    /**
     * Creates a new instance of DeploymentException with an exception message.
     * @param aMessage String describing this exception.
     */
    public DeploymentException(String aMessage)
    {
        super(aMessage);
    }

    /**
     * Creates a new instance of DeploymentException with the specified message
     * and cause.
     * @param aMessage String describing this exception.
     * @param aCause Throwable which represents an underlying problem
     * (or null).
     */
    public DeploymentException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

   /**
    * Creates a new instance of DeploymentException with the specified cause.
    * @param aCause Throwable which represents an underlying problem
    * (or null).
    */
    public DeploymentException(Throwable aCause)
    {
        super(aCause);
    }
}
