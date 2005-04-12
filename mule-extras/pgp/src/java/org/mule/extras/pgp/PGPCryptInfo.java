/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 31-mar-2005
 *
 */
package org.mule.extras.pgp;

import cryptix.pki.KeyBundle;

/**
 * @author ariva
 *
 */
public class PGPCryptInfo {

    KeyBundle keyBundle;
    boolean signRequested;
    
    public PGPCryptInfo(KeyBundle keyBundle,boolean signRequested) {
        super();
        
        this.keyBundle=keyBundle;
        this.signRequested=signRequested;
    }
    
    public KeyBundle getKeyBundle() {
        return keyBundle;
    }

    public void setKeyBundle(KeyBundle keyBundle) {
        this.keyBundle=keyBundle;
    }            
    public boolean isSignRequested() {
        return signRequested;
    }
    public void setSignRequested(boolean signRequested) {
        this.signRequested = signRequested;
    }
}
