/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice.client;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.samples.voipservice.LocaleMessage;
import org.mule.samples.voipservice.to.CreditCardTO;
import org.mule.samples.voipservice.to.CreditProfileTO;
import org.mule.samples.voipservice.to.CustomerTO;
import org.mule.samples.voipservice.to.ServiceParamTO;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.util.StringMessageUtils;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class VoipConsumer
{

    protected static transient Log logger = LogFactory.getLog(VoipConsumer.class);

    private MuleClient muleClient = null;

    public VoipConsumer() throws UMOException
    {
        init();
    }

    public VoipConsumer(String config) throws UMOException
    {
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure(config);
        init();
    }

    private void init() throws UMOException
    {
        muleClient = new MuleClient();
    }

    public void close()
    {
        muleClient.dispose();
    }

    public void requestSend(String endpoint) throws Exception
    {
        UMOMessage result;
        CustomerTO customerTO = CustomerTO.getRandomCustomer();
        CreditCardTO creditCardTO = CreditCardTO.getRandomCreditCard();
        result = muleClient.send(endpoint, new ServiceParamTO(customerTO, creditCardTO), null);
        CreditProfileTO creditProfileTO = (CreditProfileTO)((MuleMessage)result).getPayload();
        boolean valid = creditProfileTO.isValid();
        logger.info("SyncVoipConsumer.requestSend. valid = " + valid);
    }

    public static void main(String[] args)
    {
        VoipConsumer voipConsumer = null;
        int response = 0;

        try
        {
            voipConsumer = new VoipConsumer("voip-broker-sync-config.xml");

            String msg = LocaleMessage.getWelcomeMessage();

            System.out.println(StringMessageUtils.getBoilerPlate(msg, '*', 70));

            while (response != 'q')
            {
                System.out.println("\n" + LocaleMessage.getMenuOption1());
                System.out.println(LocaleMessage.getMenuOptionQuit());
                System.out.println("\n" + LocaleMessage.getMenuPromptMessage());

                response = getSelection();
                if (response == '1')
                {
                    logger.info("Sending Request...");
                    voipConsumer.requestSend("vm://VoipBrokerRequests");
                    logger.info("Request Completed.");
                }
                else if (response == 'q')
                {
                    System.out.println(LocaleMessage.getGoodbyeMessage());
                    System.exit(0);
                }
                else
                {
                    System.out.println(LocaleMessage.getMenuErrorMessage());
                }
            }

        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int getSelection() throws IOException
    {
        byte[] buf = new byte[16];
        System.in.read(buf);
        return buf[0];
    }

}
