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
 */
package org.mule.providers.dq;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.RecordFormat;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.Map;

/**
 * @author m999svm <p/> <code>DQConnector</code> A delegate provider that
 *         encapsulates a As400 DataQueue provider. The properties hostname,
 *         userId and password must be set for connection. The Message Queue
 *         location is the provider EndPoint.
 */

public class DQConnector extends AbstractServiceEnabledConnector
{
    public static final String LIB_PROPERTY = "lib";
    public static final String RECORD_DESCRIPTOR_PROPERTY = "recordDescriptor";
    private static final long DEFAULT_POLLING = 1000;
    /**
     * Pooling frequency property name*
     */
    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    private Long pollingFrequency = new Long(DEFAULT_POLLING);
    private String hostname;
    private String username;
    private String password;
    private String recordFormat = null;
    private RecordFormat format;

    private AS400 as400System = null;

    /**
     * @see org.mule.providers.AbstractConnector#createReceiver(org.mule.umo.UMOComponent,
     *      org.mule.umo.endpoint.UMOEndpoint)
     */
    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        Map props = endpoint.getProperties();
        if (props != null) {
            // Override properties on the provider for the specific endpoint
            String tempPolling = (String) props.get(PROPERTY_POLLING_FREQUENCY);
            if (tempPolling != null) {
                pollingFrequency = new Long(tempPolling);
            }
        }
        if (pollingFrequency.longValue() <= 0) {
            pollingFrequency = new Long(DEFAULT_POLLING);
        }
        logger.debug("set polling frequency to: " + pollingFrequency);

        // todo Can we include the Lib as part of the URI
        String lib = (String) endpoint.getEndpointURI().getParams().get(LIB_PROPERTY);
        logger.debug("provider endpoint: " + endpoint.getName() + " - lib: " + lib);
        String name = "";
        if (lib != null) {
            name = lib + "/";
        }

        name += endpoint.getEndpointURI().getAddress();

        DataQueue dq = new DataQueue(as400System, name);
        return serviceDescriptor.createMessageReceiver(this, component, endpoint, new Object[] { pollingFrequency, dq,
                as400System });

    }

    /**
     * @return Returns the password.
     */
    public final String getPassword()
    {
        return password;
    }

    /**
     * @param pPassword The password to set.
     */
    public void setPassword(String pPassword)
    {
        password = pPassword;
    }

    /**
     * @return Returns the pollingFrequency.
     */
    public Long getPollingFrequency()
    {
        return pollingFrequency;
    }

    /**
     * @param pPollingFrequency The pollingFrequency to set.
     */
    public void setPollingFrequency(final Long pPollingFrequency)
    {
        pollingFrequency = pPollingFrequency;
    }

    /**
     * @return Returns the system.
     */
    public AS400 getSystem()
    {
        return as400System;
    }

    /**
     * @param pSystem The system to set.
     */
    public void setSystem(final AS400 pSystem)
    {
        as400System = pSystem;
    }

    /**
     * @return Returns the hostname.
     */
    public String getHostname()
    {
        return hostname;
    }

    /**
     * @param pSystemName The hostname to set.
     */
    public void setHostname(final String pSystemName)
    {
        hostname = pSystemName;
    }

    /**
     * @return Returns the userId.
     */
    public String getUsername()
    {
        return username;
    }

    /**
     * @param username The userId to set.
     */
    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * @see org.mule.providers.AbstractConnector#doInitialise()
     */
    public void doInitialise() throws InitialisationException
    {
        super.doInitialise();
        as400System = new AS400(hostname, username, password);
        if (recordFormat != null) {
            try {
                format = DQMessageUtils.getRecordFormat(recordFormat, as400System);
            } catch (Exception e) {
                throw new InitialisationException(new Message(Messages.FAILED_LOAD_X, "Record Format: " + recordFormat),
                                                  e,
                                                  this);
            }

        }
    }

    /**
     * @see org.mule.providers.AbstractConnector#getProtocol()
     */
    public final String getProtocol()
    {
        return "dq";
    }

    /**
     * @see org.mule.providers.AbstractConnector#stopConnector()
     */

    protected void doStop() throws UMOException
    {
        as400System.disconnectAllServices();
    }

    public String getRecordFormat()
    {
        return recordFormat;
    }

    public void setRecordFormat(String recordFormat)
    {
        this.recordFormat = recordFormat;
    }

    public RecordFormat getFormat()
    {
        return format;
    }

    public void setFormat(RecordFormat format)
    {
        this.format = format;
    }

}
