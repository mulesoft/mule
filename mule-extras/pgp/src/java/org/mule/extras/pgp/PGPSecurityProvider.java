/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 21-mar-2005
 *
 */
package org.mule.extras.pgp;

import cryptix.message.Message;
import cryptix.message.MessageException;
import cryptix.message.SignedMessage;
import cryptix.pki.ExtendedKeyStore;
import cryptix.pki.KeyBundle;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.*;

import java.io.FileInputStream;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author ariva
 *  
 */
public class PGPSecurityProvider implements UMOSecurityProvider {

    private String name = "PGPSecurityProvider";

    private String publicKeyRingFileName;

    private HashMap principalsKeyBundleMap;

    private UMOSecurityContextFactory factory;
    
    /**
     * @return
     */
    public String getPublicKeyRingFileName() {
        return publicKeyRingFileName;
    }

    /**
     * @param value
     */
    public void setPublicKeyRingFileName(String value) {
        this.publicKeyRingFileName = value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOSecurityProvider#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOSecurityProvider#getName()
     */
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOSecurityProvider#authenticate(org.mule.umo.security.UMOAuthentication)
     */
    public UMOAuthentication authenticate(UMOAuthentication authentication) throws SecurityException
    {
        PGPAuthentication auth = (PGPAuthentication) authentication;

        String userId = (String) auth.getPrincipal();
        if (userId == null)
            throw new UnauthorisedException(new org.mule.config.i18n.Message(Messages.X_IS_NULL, "UserId"));

        KeyBundle userKeyBundle = (KeyBundle) principalsKeyBundleMap.get(userId);
        if (userKeyBundle == null)
            throw new UnauthorisedException(new org.mule.config.i18n.Message("pgp", 1, userId));

        Message msg = (Message) auth.getCredentials();
        if (!(msg != null && msg instanceof SignedMessage))
            throw new UnauthorisedException(new org.mule.config.i18n.Message("pgp", 2));

        try {
            if (!((SignedMessage)msg).verify(userKeyBundle))
                throw new UnauthorisedException(new org.mule.config.i18n.Message("pgp", 3));
        } catch (MessageException e) {
            throw new UnauthorisedException(new org.mule.config.i18n.Message("pgp", 4), e);
        }

        auth.setAuthenticated(true);
        auth.setDetails(userKeyBundle);

        return auth;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOSecurityProvider#supports(java.lang.Class)
     */
    public boolean supports(Class aClass) {
        return PGPAuthentication.class.isAssignableFrom(aClass);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.security.UMOSecurityProvider#createSecurityContext(org.mule.umo.security.UMOAuthentication)
     */
    public UMOSecurityContext createSecurityContext(UMOAuthentication auth) throws UnknownAuthenticationTypeException {
        return factory.create(auth);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException {
        try {
            java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
            java.security.Security.addProvider(new cryptix.openpgp.provider.CryptixOpenPGP());

            principalsKeyBundleMap = new HashMap();

            readPublicKeyRing();

            factory = new PGPSecurityContextFactory();
            
        } catch (Exception e) {
            throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X, "PGPProvider"), e);
        }

    }

    private void readPublicKeyRing() throws Exception {
        FileInputStream in = new FileInputStream(publicKeyRingFileName);

        ExtendedKeyStore ring = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
        ring.load(in, null);

        in.close();

        for (Enumeration e = ring.aliases(); e.hasMoreElements();) {
            String aliasId = (String) e.nextElement();

            KeyBundle bundle = ring.getKeyBundle(aliasId);
            if (bundle != null) {
                for (Iterator users = bundle.getPrincipals(); users.hasNext();) {
                    Principal princ = (Principal) users.next();

                    principalsKeyBundleMap.put(princ.getName(), bundle);
                }
            }
        }
    }

}