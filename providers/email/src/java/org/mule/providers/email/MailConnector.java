/* 

 * $Header$

 * $Revision$

 * $Date$

 * ------------------------------------------------------------------------------------------------------

 * 

 * Copyright (c) SymphonySoft Limited. All rights reserved.

 * http://www.symphonysoft.com

 * 

 * The software in this package is published under the terms of the BSD

 * style license a copy of which has been included with this distribution in

 * the LICENSE.txt file. 

 *

 */


package org.mule.providers.email;


import org.mule.providers.AbstractConnector;
import org.mule.umo.*;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageReceiver;


/**
 * <code>MailConnector</code> A delegate endpoint that encapsulates
 * <p/>
 * a pop3 and smtp endpoint for convenience
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MailConnector extends AbstractConnector
{
    private Pop3Connector pop3Connector = null;

    private SmtpConnector smtpConnector = null;

    /**
     * Creates a new instance of MailConnector
     */
    public MailConnector() throws Exception
    {
        dispatcherFactory = new MailMessageDispatcherFactory();
        pop3Connector = new Pop3Connector();
        smtpConnector = new SmtpConnector();
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#create()
     */

    public void doInitialise() throws InitialisationException
    {
        pop3Connector.initialise();
        smtpConnector.initialise();
    }


    /**
     * @return
     */
    public long getCheckFrequency()
    {
        return pop3Connector.getCheckFrequency();
    }

    /**
     * @return
     */
    public String getFromAddress()
    {
        return smtpConnector.getFromAddress();
    }

    /**
     * Getter for property hostname.
     *
     * @return Value of property hostname.
     */
    public String getMailbox()
    {
        return pop3Connector.getMailBox();
    }

    /* (non-Javadoc)

     * @see org.mule.providers.UMOConnector#getMessageAdapter(java.lang.Object)

     */

    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        return smtpConnector.getMessageAdapter(message);
    }

    /**
     * Getter for property password.
     *
     * @return Value of property password.
     */
    public String getSmtpPassword()
    {
        return smtpConnector.getPassword();
    }

    /**
     * Getter for property password.
     *
     * @return Value of property password.
     */
    public String getPop3Password()
    {
        return pop3Connector.getPassword();
    }

    /**
     * Getter for property hostname of the pop3 server.
     *
     * @return Value of property hostname.
     */
    public String getPop3Hostname()
    {
        return pop3Connector.getHostname();
    }

    /**
     * Getter for property hostname of the smtp server.
     *
     * @return Value of property hostname.
     */
    public String getSmtpHostname()
    {
        return smtpConnector.getHostname();
    }

    public String getProtocol()
    {
        return "pop3/smtp";
    }

    public Object getPop3Session() throws UMOException
    {
        return pop3Connector.getInbox();
    }

    public Object getSmtpSession() throws UMOException
    {
        return smtpConnector.getDispatcher("dummy").getDelegateSession();
    }

    /**
     * Getter for property username.
     *
     * @return Value of property username.
     */
    public String getSmtpUsername()
    {
        return smtpConnector.getUsername();
    }


    public String getPop3Username()
    {
        return pop3Connector.getUsername();
    }

    public int getPop3Port()
    {
        return pop3Connector.getPort();
    }

    public int getSmtpPort()
    {
        return smtpConnector.getPort();
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#registerListener(javax.jms.MessageListener, java.lang.String)
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        return pop3Connector.registerListener(component, endpoint);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#removeListener(javax.jms.MessageListener)
     */
    public void destroyReceiver(UMOMessageReceiver receiver, UMOEndpoint endpoint) throws Exception
    {
        pop3Connector.unregisterListener(receiver.getComponent(), endpoint);
        super.destroyReceiver(receiver, endpoint);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#sendEvent(org.mule.MuleEvent, org.mule.providers.MuleEndpoint)
     */
    public UMOMessage send(UMOEvent event) throws Exception
    {
        return smtpConnector.getDispatcher("dummy").send(event);
    }

    /**
     * @param l
     */
    public void setCheckFrequency(long l)
    {
        pop3Connector.setCheckFrequency(l);
    }

    /**
     * @param from
     */
    public void setFromAddress(String from)
    {
        smtpConnector.setFromAddress(from);
    }

    public void setPop3Port(int port) {
        pop3Connector.setPort(port);
    }

    public void setSmtpPort(int port) {
        smtpConnector.setPort(port);
    }

    /**
     * Setter for property password.
     *
     * @param password New value of property password.
     */
    public void setPop3Password(String password)
    {
        pop3Connector.setPassword(password);
    }

    /**
     * Setter for property password.
     *
     * @param password New value of property password.
     */
    public void setSmtpPassword(String password)
    {
        smtpConnector.setPassword(password);
    }

    /**
     * Setter for property hostname of the pop3 server.
     *
     * @param hostname New value of property hostname.
     */
    public void setPop3Hostname(String hostname)
    {
        pop3Connector.setHostname(hostname);
    }

    /**
     * Setter for property hostname of the smtp server.
     *
     * @param hostname New value of property hostname.
     */
    public void setSmtpHostname(String hostname)
    {
        smtpConnector.setHostname(hostname);
    }


    /**
     * Setter for property username.
     *
     * @param username New value of property username.
     */
    public void setPop3Username(String username)
    {
        pop3Connector.setUsername(username);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#start()
     */
    public void startConnector() throws UMOException
    {
        //need to call base start() method to make sure all
        //relivant code is called
        smtpConnector.start();
        pop3Connector.start();
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#stop()
     */
    public void stopConnector() throws UMOException
    {
        //need to call base stop() method to make sure all
        //relivant code is called
        smtpConnector.stop();
        pop3Connector.stop();
    }

    /**
     * @param user
     */
    public void setSmtpUsername(String user)
    {
        smtpConnector.setUsername(user);
    }

    /* (non-Javadoc)
     * @see org.mule.providers.AbstractConnector#disposeConnector()
     */
    protected void disposeConnector()
    {
        //need to call base shutdown() method to make sure all
        //relivant code is called
        smtpConnector.dispose();
        pop3Connector.dispose();
    }

    /**
     * @return
     */
    public String getBccAddresses()
    {
        return smtpConnector.getBccAddresses();
    }

    /**
     * @return
     */
    public String getCcAddresses()
    {
        return smtpConnector.getCcAddresses();
    }

    /**
     * @return
     */
    public String getSubject()
    {
        return smtpConnector.getSubject();
    }

    /**
     * @param bcc
     */
    public void setBccAddresses(String bcc)
    {
        smtpConnector.setBccAddresses(bcc);
    }

    /**
     * @param cc
     */
    public void setCcAddresses(String cc)
    {
        smtpConnector.setCcAddresses(cc);
    }

    /**
     * @param subject
     */
    public void setSubject(String subject)
    {
        smtpConnector.setSubject(subject);
    }

    /**
     * @return Returns the pop3Connector.
     */
    protected Pop3Connector getPop3Connector()
    {
        return pop3Connector;
    }

    /**
     * @return Returns the smtpConnector.
     */
    protected SmtpConnector getSmtpConnector()
    {
        return smtpConnector;
    }
}