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
package org.mule.extras.pgp;

import cryptix.message.EncryptedMessageBuilder;
import cryptix.message.LiteralMessageBuilder;
import cryptix.message.Message;
import cryptix.message.MessageException;
import cryptix.message.SignedMessageBuilder;
import cryptix.openpgp.PGPArmouredMessage;
import cryptix.pki.ExtendedKeyStore;
import cryptix.pki.KeyBundle;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * @author ariva
 * 
 */
public class GenerateTestMessage
{
    private static ExtendedKeyStore clientPublicRing, clientPrivateRing;
    private static ExtendedKeyStore serverPublicRing, serverPrivateRing;

    private static KeyBundle serverPublicKey;
    private static KeyBundle clientPrivateKey;

    public static void readKeyrings()
    {
        clientPublicRing = readKeyRing("clientPublic.gpg");
        clientPrivateRing = readKeyRing("clientPrivate.gpg");
        serverPublicRing = readKeyRing("serverPublic.gpg");
        serverPrivateRing = readKeyRing("serverPrivate.gpg");
    }

    public static ExtendedKeyStore readKeyRing(String filename)
    {

        ExtendedKeyStore ring = null;

        try {

            FileInputStream in = new FileInputStream(filename);

            ring = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
            ring.load(in, null);

            in.close();

        } catch (IOException ioe) {
            System.err.println("IOException... You did remember to run the "
                    + "GenerateAndWriteKey example first, right?");
            ioe.printStackTrace();
            System.exit(-1);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP KeyRing. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (KeyStoreException kse) {
            System.err.println("Reading keyring failed.");
            kse.printStackTrace();
            System.exit(-1);
        } catch (CertificateException ce) {
            System.err.println("Reading keyring failed.");
            ce.printStackTrace();
            System.exit(-1);
        }

        return ring;
    }

    public static KeyBundle findKeyBundle(ExtendedKeyStore ring, String principal) throws Exception
    {

        for (Enumeration e = ring.aliases(); e.hasMoreElements();) {
            String aliasId = (String) e.nextElement();
            KeyBundle bundle = ring.getKeyBundle(aliasId);
            if (bundle != null) {
                for (Iterator users = bundle.getPrincipals(); users.hasNext();) {
                    Principal princ = (Principal) users.next();
                    System.out.println("aliasId:" + aliasId + ", user:" + princ.toString());
                    if (princ.toString().equals(principal)) {
                        return bundle;
                    }
                }
            }
        }

        throw new Exception("KeyBundle not found for " + principal);
    }

    public static void decodeKeyRings() throws Exception
    {
        serverPublicKey = findKeyBundle(clientPublicRing, "Mule server <mule_server@mule.com>");
        clientPrivateKey = findKeyBundle(clientPrivateRing, "Mule client <mule_client@mule.com>");
        System.out.println("Server private keyring:");
        findKeyBundle(serverPrivateRing, "Mule server <mule_server@mule.com>");
    }

    public static void writeMsg()
    {
        Message msg = null;

        try {
            String data = "This is a test message.\n" + "This is another line.\n";
            LiteralMessageBuilder lmb = LiteralMessageBuilder.getInstance("OpenPGP");
            lmb.init(data);
            msg = lmb.build();
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP LiteralMessageBuilder."
                    + " This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (MessageException me) {
            System.err.println("Creating the literal message failed.");
            me.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Sign the message.
        //
        // Note that signing usually comes before encryption, such that
        // unauthorized parties cannot see who signed the message.
        // **********************************************************************
        try {

            SignedMessageBuilder smb = SignedMessageBuilder.getInstance("OpenPGP");

            // use the following line for compatibility with older PGP versions

            // SignedMessageBuilder smb =
            // SignedMessageBuilder.getInstance("OpenPGP/V3");

            smb.init(msg);
            smb.addSigner(clientPrivateKey, "TestingPassphrase".toCharArray());

            msg = smb.build();

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP SignedMessageBuilder. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (UnrecoverableKeyException uke) {
            System.err.println("Incorrect passphrase.");
            uke.printStackTrace();
            System.exit(-1);
        } catch (MessageException me) {
            System.err.println("Generating the message failed.");
            me.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Armour the message and write it to disk
        // **********************************************************************
        try {

            PGPArmouredMessage armoured;

            armoured = new PGPArmouredMessage(msg);
            FileOutputStream out = new FileOutputStream("signed.asc");
            out.write(armoured.getEncoded());
            out.close();

        } catch (MessageException me) {
            System.err.println("Writing the encrypted message failed.");
            me.printStackTrace();
            System.exit(-1);
        } catch (IOException ioe) {
            System.err.println("Writing the encrypted message failed.");
            ioe.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Encrypt the message.
        // **********************************************************************
        try {

            EncryptedMessageBuilder emb = EncryptedMessageBuilder.getInstance("OpenPGP");
            emb.init(msg);
            emb.addRecipient(serverPublicKey);
            msg = emb.build();

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP " + "EncryptedMessageBuilder. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (MessageException me) {
            System.err.println("Creating the encrypted message failed.");
            me.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Armour the message and write it to disk
        // **********************************************************************
        try {

            PGPArmouredMessage armoured;

            armoured = new PGPArmouredMessage(msg);
            FileOutputStream out = new FileOutputStream("encrypted-signed.asc");
            out.write(armoured.getEncoded());
            out.close();

        } catch (MessageException me) {
            System.err.println("Writing the encrypted message failed.");
            me.printStackTrace();
            System.exit(-1);
        } catch (IOException ioe) {
            System.err.println("Writing the encrypted message failed.");
            ioe.printStackTrace();
            System.exit(-1);
        }

    }

    public static void main(String[] args) throws Exception
    {
        java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
        java.security.Security.addProvider(new cryptix.openpgp.provider.CryptixOpenPGP());

        readKeyrings();
        decodeKeyRings();

        writeMsg();
    }
}
