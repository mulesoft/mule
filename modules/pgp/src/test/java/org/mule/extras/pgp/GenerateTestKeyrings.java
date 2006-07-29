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
package org.mule.extras.pgp;

import cryptix.openpgp.PGPKeyBundle;
import cryptix.pki.CertificateBuilder;
import cryptix.pki.ExtendedKeyStore;
import cryptix.pki.KeyBundleException;
import cryptix.pki.KeyBundleFactory;
import cryptix.pki.PrincipalBuilder;
import cryptix.pki.PrincipalException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * @author ariva
 * 
 */
public class GenerateTestKeyrings
{
    private static PGPKeyBundle serverPublicKey, serverPrivateKey;
    private static PGPKeyBundle clientPublicKey, clientPrivateKey;

    public static void generateServerKey()
    {

        SecureRandom sr = new SecureRandom();

        try {

            KeyBundleFactory kbf = KeyBundleFactory.getInstance("OpenPGP");

            serverPublicKey = (PGPKeyBundle) kbf.generateEmptyKeyBundle();
            serverPrivateKey = (PGPKeyBundle) kbf.generateEmptyKeyBundle();

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP KeyBundleFactory. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (KeyBundleException kbe) {
            System.err.println("Generating an empty KeyBundle failed.");
            kbe.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Now generate the signing key.
        //
        // We'll use a 1024 bit DSA key here. You can use other algorithms if
        // you want, by using for example:
        // kpg = KeyPairGenerator.getInstance("OpenPGP/Signing/RSA");
        // kpg = KeyPairGenerator.getInstance("OpenPGP/Signing/ElGamal");
        //
        // (Note that ElGamal signature were not supported yet at the time of
        // writing this example class.)
        // **********************************************************************
        KeyPairGenerator kpg = null;

        try {

            kpg = KeyPairGenerator.getInstance("OpenPGP/Signing/RSA");

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP KeyPairGenerator. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        }

        kpg.initialize(1024, sr);
        KeyPair kp = kpg.generateKeyPair();

        PublicKey pubkey = kp.getPublic();
        PrivateKey privkey = kp.getPrivate();

        // **********************************************************************
        // Now build the primary userid for this key.
        // **********************************************************************
        Principal userid = null;

        try {

            PrincipalBuilder princbuilder = PrincipalBuilder.getInstance("OpenPGP/UserID");

            userid = princbuilder.build("Mule server <mule_server@mule.com>");

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP PrincipalBuilder. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (PrincipalException pe) {
            System.err.println("Generating the user id failed.");
            pe.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // We need to sign the generated user id with our key, which will
        // bring us a so called 'certificate'.
        //
        // This btw is a self-signed certificate, you can also have certificates
        // signed by other people. See the ReadAndSignKey example for that.
        // **********************************************************************
        Certificate cert = null;

        try {

            CertificateBuilder certbuilder = CertificateBuilder.getInstance("OpenPGP/Self");

            cert = certbuilder.build(pubkey, userid, privkey, sr);

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP CertificateBuilder. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (CertificateException ce) {
            System.err.println("Generating the self certification sig failed.");
            ce.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Building up the keybundle is easy now. We only need to add the
        // certificate, as this will automagically add the public key and the
        // user id to the keybundle.
        // **********************************************************************
        try {

            serverPublicKey.addCertificate(cert);
            serverPrivateKey.addCertificate(cert);

        } catch (KeyBundleException kbe) {
            System.err.println("Adding the self certificate to the keybundle failed.");
            kbe.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Of course we still need to add the private key to the private
        // keybundle, while encrypting it with a passphrase.
        // **********************************************************************
        try {

            serverPrivateKey.addPrivateKey(privkey, pubkey, "TestingPassphrase".toCharArray(), sr);

        } catch (KeyBundleException kbe) {
            System.err.println("Adding the private key to the keybundle failed.");
            kbe.printStackTrace();
            System.exit(-1);
        }

    }

    public static void generateClientKey()
    {

        SecureRandom sr = new SecureRandom();

        try {

            KeyBundleFactory kbf = KeyBundleFactory.getInstance("OpenPGP");

            clientPublicKey = (PGPKeyBundle) kbf.generateEmptyKeyBundle();
            clientPrivateKey = (PGPKeyBundle) kbf.generateEmptyKeyBundle();

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP KeyBundleFactory. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (KeyBundleException kbe) {
            System.err.println("Generating an empty KeyBundle failed.");
            kbe.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Now generate the signing key.
        //
        // We'll use a 1024 bit DSA key here. You can use other algorithms if
        // you want, by using for example:
        // kpg = KeyPairGenerator.getInstance("OpenPGP/Signing/RSA");
        // kpg = KeyPairGenerator.getInstance("OpenPGP/Signing/ElGamal");
        //
        // (Note that ElGamal signature were not supported yet at the time of
        // writing this example class.)
        // **********************************************************************
        KeyPairGenerator kpg = null;

        try {

            kpg = KeyPairGenerator.getInstance("OpenPGP/Signing/RSA");

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP KeyPairGenerator. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        }

        kpg.initialize(1024, sr);
        KeyPair kp = kpg.generateKeyPair();

        PublicKey pubkey = kp.getPublic();
        PrivateKey privkey = kp.getPrivate();

        // **********************************************************************
        // Now build the primary userid for this key.
        // **********************************************************************
        Principal userid = null;

        try {

            PrincipalBuilder princbuilder = PrincipalBuilder.getInstance("OpenPGP/UserID");

            userid = princbuilder.build("Mule client <mule_client@mule.com>");

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP PrincipalBuilder. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (PrincipalException pe) {
            System.err.println("Generating the user id failed.");
            pe.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // We need to sign the generated user id with our key, which will
        // bring us a so called 'certificate'.
        //
        // This btw is a self-signed certificate, you can also have certificates
        // signed by other people. See the ReadAndSignKey example for that.
        // **********************************************************************
        Certificate cert = null;

        try {

            CertificateBuilder certbuilder = CertificateBuilder.getInstance("OpenPGP/Self");

            cert = certbuilder.build(pubkey, userid, privkey, sr);

        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("Cannot find the OpenPGP CertificateBuilder. "
                    + "This usually means that the Cryptix OpenPGP provider is not " + "installed correctly.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (CertificateException ce) {
            System.err.println("Generating the self certification sig failed.");
            ce.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Building up the keybundle is easy now. We only need to add the
        // certificate, as this will automagically add the public key and the
        // user id to the keybundle.
        // **********************************************************************
        try {

            clientPublicKey.addCertificate(cert);
            clientPrivateKey.addCertificate(cert);

        } catch (KeyBundleException kbe) {
            System.err.println("Adding the self certificate to the keybundle failed.");
            kbe.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Of course we still need to add the private key to the private
        // keybundle, while encrypting it with a passphrase.
        // **********************************************************************
        try {

            clientPrivateKey.addPrivateKey(privkey, pubkey, "TestingPassphrase".toCharArray(), sr);

        } catch (KeyBundleException kbe) {
            System.err.println("Adding the private key to the keybundle failed.");
            kbe.printStackTrace();
            System.exit(-1);
        }

    }

    public static void writeKeyrings()
    {
        ExtendedKeyStore clientPubRing = null;
        ExtendedKeyStore clientPrivRing = null;
        ExtendedKeyStore serverPubRing = null;
        ExtendedKeyStore serverPrivRing = null;

        try {

            clientPubRing = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
            clientPrivRing = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
            serverPubRing = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
            serverPrivRing = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");

        } catch (KeyStoreException kse) {
            System.err.println("KeyStoreException on creating a keyring. "
                    + "This means that the KeyStore implementation could not be "
                    + "found and that there is potentially a problem with the " + "provider");
            kse.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Before using them, KeyStore objects have to be initialized.
        // Because we want to create empty keystores, we are going to
        // initialize them with null arguments.
        // Unfortunately we have to catch all sorts of exceptions that are
        // impossible to happen.
        // **********************************************************************

        try {

            clientPubRing.load(null, null);
            clientPrivRing.load(null, null);
            serverPubRing.load(null, null);
            serverPrivRing.load(null, null);

        } catch (IOException ioe) {
            System.err.println("KeyStoreException on keyring init. "
                    + "There should be no way for this exception to turn up.");
            ioe.printStackTrace();
            System.exit(-1);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("NoSuchAlgorithmException on keyring init. "
                    + "There should be no way for this exception to turn up.");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (CertificateException ce) {
            System.err.println("CertificateException on keyring init. "
                    + "There should be no way for this exception to turn up.");
            ce.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Filling up the keyrings is pretty easy now. Just add the right
        // keybundles to the rings.
        // **********************************************************************

        try {

            clientPubRing.setKeyBundleEntry(clientPublicKey);
            clientPubRing.setKeyBundleEntry(serverPublicKey);

            clientPrivRing.setKeyBundleEntry(clientPrivateKey);

            serverPubRing.setKeyBundleEntry(clientPublicKey);
            serverPubRing.setKeyBundleEntry(serverPublicKey);

            serverPrivRing.setKeyBundleEntry(serverPrivateKey);

        } catch (KeyStoreException kse) {
            System.err.println("KeyStoreException on adding a key.");
            kse.printStackTrace();
            System.exit(-1);
        }

        // **********************************************************************
        // Now we're going to write the keyring files.
        // The second argument of the store method is a passphrase. Because
        // PGP keyrings are stored unencrypted, a 'null' value has to be
        // provided here.
        // **********************************************************************
        FileOutputStream out;

        try {

            out = new FileOutputStream("clientPublic.gpg"); // pkr = public key
            // ring
            clientPubRing.store(out, null);
            out.close();

            out = new FileOutputStream("clientPrivate.gpg"); // skr = secret
            // key ring
            clientPrivRing.store(out, null);
            out.close();

            out = new FileOutputStream("serverPublic.gpg");
            serverPubRing.store(out, null);
            out.close();

            out = new FileOutputStream("serverPrivate.gpg");
            serverPrivRing.store(out, null);
            out.close();

        } catch (IOException ioe) {
            System.err.println("IOException on writing a keyring.");
            ioe.printStackTrace();
            System.exit(-1);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("NoSuchAlgorithmException on writing a keyring."
                    + " Given that no encryption is used while writing the keystore, " + "this should not happen!");
            nsae.printStackTrace();
            System.exit(-1);
        } catch (CertificateException ce) {
            System.err.println("CertificateException on writing a keyring.");
            ce.printStackTrace();
            System.exit(-1);
        } catch (KeyStoreException kse) {
            System.err.println("KeyStoreException on writing a keyring.");
            kse.printStackTrace();
            System.exit(-1);
        }

    }

    public static void main(String[] args)
    {
        java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
        java.security.Security.addProvider(new cryptix.openpgp.provider.CryptixOpenPGP());

        generateServerKey();
        generateClientKey();

        writeKeyrings();
    }
}
