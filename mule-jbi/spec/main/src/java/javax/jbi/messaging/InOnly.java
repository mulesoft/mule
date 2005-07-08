// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * InOnly.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

/** Supports operations used to process an In Only MEP to completion.
 *
 * @author JSR208 Expert Group
 */
public interface InOnly extends MessageExchange
{
    /** Retrieves the <i>in</i> normalized message from this exchange.
     *  @return in message
     */
    NormalizedMessage getInMessage();
        
    /** Sets the <i>in</i> normalized message for this exchange.
     *  @param msg in message
     *  @throws MessagingException unable to set in message
     */
    void setInMessage(NormalizedMessage msg)
        throws MessagingException;
}
