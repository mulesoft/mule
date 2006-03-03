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

package org.mule.providers.soap.xfire.transport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.soap.SoapTransport;
import org.codehaus.xfire.soap.SoapTransportHelper;
import org.codehaus.xfire.transport.AbstractTransport;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.DefaultEndpoint;
import org.codehaus.xfire.transport.MapSession;
import org.codehaus.xfire.transport.Session;
import org.mule.umo.manager.UMOWorkManager;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleLocalTransport extends AbstractTransport implements SoapTransport
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public final static String BINDING_ID = "urn:xfire:transport:local";
    public final static String URI_PREFIX = "xfire.local://";
    private Session session;
    private boolean maintainSession;
    protected UMOWorkManager workManager;

    public MuleLocalTransport(UMOWorkManager workManager)
    {
        super();
        SoapTransportHelper.createSoapTransport(this);
        this.workManager = workManager;
    }

    protected Channel createNewChannel(String uri)
    {
        logger.debug("Creating new channel for uri: " + uri);

        MuleLocalChannel c = new MuleLocalChannel(uri, this, session);
        c.setWorkManager(workManager);
        c.setEndpoint(new DefaultEndpoint());

        return c;
    }

    public void setMaintainSession(boolean maintainSession)
    {
        this.maintainSession = maintainSession;
        resetSession();
    }

    public void resetSession()
    {
        if (maintainSession) {
            session = new MapSession();
        }
        else {
            session = null;
        }
    }

    protected String getUriPrefix()
    {
        return URI_PREFIX;
    }

    public String[] getSupportedBindings()
    {
        return new String[]{BINDING_ID};
    }

    public String[] getKnownUriSchemes()
    {
        return new String[]{URI_PREFIX};
    }

    public String getName()
    {
        return "Local";
    }

    public String[] getSoapTransportIds()
    {
        return new String[]{BINDING_ID};
    }
}