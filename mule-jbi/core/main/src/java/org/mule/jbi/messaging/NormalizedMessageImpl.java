/*
 * Copyright 2005 SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * ------------------------------------------------------------------------------------------------------
 * $Header$
 * $Revision$
 * $Date$
 */
package org.mule.jbi.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.security.auth.Subject;
import javax.xml.transform.Source;

/**
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 */
public class NormalizedMessageImpl implements NormalizedMessage {

	private Source content;
	private Map attachments;
	private Map properties;
	private Subject securitySubject;
	
	public NormalizedMessageImpl() {
		this.attachments = new HashMap();
		this.properties = new HashMap();
	}
	
	public void addAttachment(String id, DataHandler content) throws MessagingException {
		this.attachments.put(id, content);
	}

	public Source getContent() {
		return this.content;
	}

	public DataHandler getAttachment(String id) {
		return (DataHandler) this.attachments.get(id);
	}

	public Set getAttachmentNames() {
		return this.attachments.keySet();
	}

	public void removeAttachment(String id) throws MessagingException {
		this.attachments.remove(id);
	}

	public void setContent(Source content) throws MessagingException {
		this.content = content;
	}

	public void setProperty(String name, Object value) {
		this.properties.put(name, value);
	}

	public void setSecuritySubject(Subject subject) {
		this.securitySubject = subject;
	}

	public Set getPropertyNames() {
		return this.properties.keySet();
	}

	public Object getProperty(String name) {
		return this.properties.get(name);
	}

	public Subject getSecuritySubject() {
		return this.securitySubject;
	}

}
