// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * InOptionalOut.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

/** Supports operations used to process an In Optional Out MEP to completion.
 *
 * @author JSR208 Expert Group
 */
public interface InOptionalOut extends MessageExchange
{
    /** Retrieves the "in" message reference from this exchange.
     *  @return in message
     */
    NormalizedMessage getInMessage();
    
    /** Retrieves the "out" message reference from this exchange.
     *  @return out message
     */
    NormalizedMessage getOutMessage();
    
    /** Specifies the "in" message reference for this exchange.
     *  @param msg in message
     *  @throws MessagingException unable to set in message
     */
    void setInMessage(NormalizedMessage msg)
        throws MessagingException;
    
    /** Specifies the "out" message reference for this exchange.
     *  @param msg out message
     *  @throws MessagingException unable to set out message
     */
    void setOutMessage(NormalizedMessage msg)
        throws MessagingException;
}
