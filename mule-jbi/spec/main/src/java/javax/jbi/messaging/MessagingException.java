// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * MessagingException.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

/** Generic exception used to report messaging related errors in the Normalized 
 *  Message Service.
 *
 * @author JSR208 Expert Group
 */
public class MessagingException extends javax.jbi.JBIException
{
    /** Create a new MessagingException.
     * @param msg error detail
     */
    public MessagingException(String msg)
    {
        super(msg);
    }
    
    /** Create a new MessagingException with the specified cause and error text.
     * @param msg error detail
     * @param cause underlying error
     */
    public MessagingException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
    
    /** Create a new MessagingException with the specified cause.
     * @param cause underlying error
     */   
    public MessagingException(Throwable cause)
    {
        super(cause);
    }
}
