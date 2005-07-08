// Copyright (c) 2004-2005 Sun Microsystems Inc., All Rights Reserved.
/*
 * NormalizedMessage.java
 *
 * SUN PROPRIETARY/CONFIDENTIAL.
 * This software is the proprietary information of Sun Microsystems, Inc.  
 * Use is subject to license terms.
 * 
 */

package javax.jbi.messaging;

import java.util.Set;

import javax.activation.DataHandler;
import javax.security.auth.Subject;
import javax.xml.transform.Source;

/** Represents a JBI Normalized Message. 
 *
 * @author JSR208 Expert Group
 */
public interface NormalizedMessage
{
    /** Add an attachment to the message.
     * @param id unique identifier for the attachment
     * @param content attachment content
     * @throws MessagingException failed to add attachment
     */
    void addAttachment(String id, DataHandler content)
        throws MessagingException;
    
    /** Retrieve the content of the message.
     *  @return message content
     */
    Source getContent();
    
    /** Retrieve attachment with the specified identifier.
     *  @param id unique identifier for attachment
     *  @return DataHandler representing attachment content, or null if an
     *  attachment with the specified identifier is not found
     */
    DataHandler getAttachment(String id);
    
    /** Returns a list of identifiers for each attachment to the message.
     *  @return iterator over String attachment identifiers
     */
    Set getAttachmentNames();
    
    /** Removes attachment with the specified unique identifier.
     *  @param id attachment identifier
     *  @throws MessagingException failed to remove attachment
     */
    void removeAttachment(String id)
        throws MessagingException;
    
    /** Set the content of the message.
     *  @param content message content
     *  @throws MessagingException failed to set content
     */
    void setContent(Source content)
        throws MessagingException;
    
    /** Set a property on the message.
     *  @param name property name
     *  @param value property value
     */
    void setProperty(String name, Object value);
    
    /**
     * Set the security Subject for the message.
     * @param subject Subject to associated with message.
     */
    void setSecuritySubject(Subject subject);
    
    /** Retrieve a list of property names for the message.
     *  @return list of property names
     */  
    Set getPropertyNames();
        
    /** Retrieve a property from the message.
     *  @param name property name
     *  @return property value, or null if the property does not exist
     */
    Object getProperty(String name);
    
    /** Retrieve the security Subject from the message.
     *  @return security Subject associated with message, or null.
     */
    Subject getSecuritySubject();
}
