// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * JBIException.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 */

package javax.jbi;

/**
 * JBIException is the top-level exception thrown by all JBI system
 * components.
 *
 * @author JSR208 Expert Group
 */

public class JBIException extends Exception
{
    /**
     * Creates a new instance of JBIException with an exception message.
     * @param aMessage String describing this exception.
     */
    public JBIException(String aMessage)
    {
        super(aMessage);
    }

    /**
     * Creates a new instance of JBIException with the specified message
     * and cause.
     * @param aMessage String describing this exception.
     * @param aCause Throwable which represents an underlying problem
     * (or null).
     */
    public JBIException(String aMessage, Throwable aCause)
    {
        super(aMessage, aCause);
    }

   /**
    * Creates a new instance of JBIException with the specified cause.
    * @param aCause Throwable which represents an underlying problem
    * (or null).
    */
    public JBIException(Throwable aCause)
    {
        super(aCause);
    }
}
