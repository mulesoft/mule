/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.tools.benchmark;

import org.mule.umo.UMOException;

/**
 * <code>SenderAndReceiver</code> runs a sender and reciever in the same VM
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SenderAndReceiver
{
    private Receiver receiver;
    private Sender sender;


    public static void main(String[] args)
    {
        try
        {
            SenderAndReceiver senderAndReceiver = new SenderAndReceiver(new RunnerConfig(args));
            senderAndReceiver.start();
        } catch (Throwable e)
        {
            e.printStackTrace();
        }
    }

    public SenderAndReceiver(RunnerConfig config) throws Exception
    {
        sender = new Sender(config);
        receiver = new Receiver(config);
    }

    public void start() throws Exception {
        receiver.register();
        receiver.start();
        sender.start();
        sender.send();
    }

}
