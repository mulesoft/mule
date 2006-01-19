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
package org.mule.providers.gs;

import org.mule.providers.gs.space.GSSpaceFactory;
import org.mule.providers.space.SpaceConnector;
import org.mule.providers.space.TransactedSpaceMessageReceiver;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.Iterator;

/**
 * Provides a Space connector to be used with the GigaSpaces JavaSpaces implementation
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GSConnector extends SpaceConnector {

    private long transactionTimeout = 32 * 1000;

    public GSConnector() {
        registerSupportedProtocol("rmi");
        registerSupportedProtocol("java");
        setSpaceFactory(new GSSpaceFactory());
    }

    public void doInitialise() throws InitialisationException {
        super.doInitialise();

        //NOTE: This code right now only sets the transaction timeout as the Space is null until
        //The receiver hasbeen connected.  Need to find a clena way of setting the Jini Transaction
        //Manager on the factory
        for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();) {
            UMOMessageReceiver receiver = (UMOMessageReceiver) iterator.next();
            if (receiver instanceof TransactedSpaceMessageReceiver) {
//                GSSpace space = (GSSpace) ((TransactedSpaceMessageReceiver) receiver).getSpace();
//                try {
//                    transactionManager = (LocalTransactionManager)
//                            LocalTransactionManager.getInstance((IJSpace) space.getJavaSpace());
//                } catch (RemoteException e) {
//                    throw new LifecycleException(e, this);
//                }

                //Because Jini uses its own transaction management we need to set the Manager on the
                //Transaction Factory
                JiniTransactionFactory factory = (JiniTransactionFactory) receiver.getEndpoint().getTransactionConfig().getFactory();
                //factory.setTransactionManager(transactionManager);
                factory.setTransactionTimeout(transactionTimeout);
            }
        }
    }


    public String getProtocol() {
        return "gs";
    }

    public long getTransactionTimeout() {
        return transactionTimeout;
    }

    public void setTransactionTimeout(long transactionTimeout) {
        this.transactionTimeout = transactionTimeout;
    }
}
