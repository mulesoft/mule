/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.gs.space;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.LocalTransactionManager;
import net.jini.core.entry.Entry;
import net.jini.core.transaction.server.TransactionManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.space.CreateSpaceException;
import org.mule.providers.gs.GSConnector;
import org.mule.providers.gs.JiniMessage;
import org.mule.providers.gs.JiniTransactionFactory;
import org.mule.providers.gs.filters.JavaSpaceTemplateFilter;
import org.mule.umo.UMOFilter;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceException;
import org.mule.umo.space.UMOSpaceFactory;

/**
 * Creates a GigiSpaces JavaSpace
 *
 * @see GSSpace
 * @see net.jini.space.JavaSpace
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GSSpaceFactory implements UMOSpaceFactory {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private boolean enableMonitorEvents = true;

    public UMOSpace create(String spaceIdentifier) throws UMOSpaceException {
        if(spaceIdentifier==null) {
            throw new NullPointerException(new Message(Messages.X_IS_NULL, "spaceIdentifier").toString());
        }
        try {
            GSSpace space = new GSSpace(spaceIdentifier, enableMonitorEvents);
            space.setEntryTemplate(createDefaultEntry(spaceIdentifier));
            return space;

        } catch (FinderException e) {
            throw new CreateSpaceException(e);
        }
    }

    /**
     * Creates a space based on the endpoint URI and can use additional properties, transaction info,
     * security info and filters associated with the endpoint
     *
     * @param endpoint the endpoint from which to construct the space
     * @return an new Space object
     * @throws org.mule.umo.space.UMOSpaceException
     *
     */
    public UMOSpace create(UMOImmutableEndpoint endpoint) throws UMOSpaceException {
        try {
            //Todo the Spaces themselves are keyed on endpoint + any filtering information. Should that
            //info also be included in the space name??
            GSSpace space = new GSSpace(endpoint.getEndpointURI().toString(), enableMonitorEvents);

            //Because Jini uses its own transaction management we need to set the Manager on the
            //Transaction Factory
            if(endpoint.getTransactionConfig().getFactory()!=null) {
                JiniTransactionFactory txFactory = (JiniTransactionFactory)endpoint.getTransactionConfig().getFactory();
                TransactionManager transactionManager = LocalTransactionManager.getInstance((IJSpace) space.getJavaSpace());
                txFactory.setTransactionManager(transactionManager);
                txFactory.setTransactionTimeout(((GSConnector)endpoint.getConnector()).getTransactionTimeout());
                space.setTransactionFactory(txFactory);
            }

            //We do not need to set an entry template if we are not recieving messages from the space
            if(!endpoint.getType().equalsIgnoreCase(UMOEndpoint.ENDPOINT_TYPE_SENDER)) {
                //Now set the Entry template on the space
                if(endpoint.getFilter()!=null) {
                    UMOFilter filter = endpoint.getFilter();
                    if(filter instanceof JavaSpaceTemplateFilter) {
                        space.setEntryTemplate(((JavaSpaceTemplateFilter)filter).getEntry());
                    } else {
                        if(!endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER)) {
                            logger.warn("Filter on endpoint " + endpoint.getEndpointURI().toString() + " Was not a JiniEntryFilter. Endpoint will match all entries of all types for this endpoint");
                        }
                        space.setEntryTemplate(createDefaultEntry(endpoint.getEndpointURI().toString()));
                    }
                } else {
                    if(!endpoint.getType().equals(UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER)) {
                        logger.warn("Filter on endpoint " + endpoint.getEndpointURI().toString() + " Was not set. Endpoint will match all entries of all types for this endpoint");
                    }
                    space.setEntryTemplate(createDefaultEntry(endpoint.getEndpointURI().toString()));
                }
            }
            return space;
        } catch (Exception e) {
            throw new CreateSpaceException(e);
        }
    }

    protected Entry createDefaultEntry(String identifier) {
        //return new ExternalEntry(identifier, null);
        return new JiniMessage();
    }
}
