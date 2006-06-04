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

package org.mule.samples.voipservice.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.samples.voipservice.to.CreditCardTO;
import org.mule.samples.voipservice.to.CreditProfileTO;
import org.mule.samples.voipservice.to.CustomerTO;
import org.mule.samples.voipservice.to.ServiceParamTO;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.util.StringMessageUtils;

import java.io.IOException;

/**
 * @author Binildas Christudas
 */
public class VoipConsumer {

    protected static transient Log logger = LogFactory.getLog(VoipConsumer.class);

    private MuleClient muleClient = null;

    public VoipConsumer() throws UMOException {
        init();
    }

    public VoipConsumer(String config) throws UMOException {

        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure(config);
        init();
    }

    private void init() throws UMOException {
        muleClient = new MuleClient();
    }

    public void close() {
        MuleManager.getInstance().dispose();
    }

    public void requestSend(String endpoint) throws Exception {
        UMOMessage result;
        CustomerTO customerTO = CustomerTO.getRandomCustomer();
        CreditCardTO creditCardTO = CreditCardTO.getRandomCreditCard();
        result = muleClient.send(endpoint, new ServiceParamTO(customerTO, creditCardTO), null);
        CreditProfileTO creditProfileTO = (CreditProfileTO) ((MuleMessage) result).getPayload();
        boolean valid = creditProfileTO.isValid();
        logger.info("SyncVoipConsumer.requestSend. valid = " + valid);
    }

    public static void main(String[] args) {
        VoipConsumer voipConsumer = null;
        int response = 0;

        try {
            voipConsumer = new VoipConsumer("voip-broker-sync-config.xml");

            String msg = "Welcome to the Mule Voip Services Provisioning Example."
                + " This example was published as part of a featured article on java.net"
                + " titled 'Service Provisioning Through ESB' (http://today.java.net/lpt/a/233).";

            System.out.println(StringMessageUtils.getBoilerPlate(msg, '*', 70));

            while (response != 'q') {
                System.out.println("\n[1] make a service request");
                System.out.println("[q] quit");
                System.out.println("\nPlease make your selection: ");

                response = getSelection();
                if (response == '1') {
                    logger.info("Sending Request...");
                    voipConsumer.requestSend("vm://VoipBrokerRequests");
                    logger.info("Request Completed.");
                } else if (response == 'q') {
                    System.out.println("Bye");
                    System.exit(0);
                } else {
                    System.out.println("That response is not recognised, try again:");
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int getSelection() throws IOException {
        byte[] buf = new byte[16];
        System.in.read(buf);
        return buf[0];
    }

}
