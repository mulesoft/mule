/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 21-mar-2005
 *
 */
package org.mule.extras.pgp;

import cryptix.message.Message;
import cryptix.pki.KeyBundle;
import org.mule.umo.security.UMOAuthentication;

/**
 * @author ariva
 *
 */
public class PGPAuthentication implements UMOAuthentication {

    boolean authenticated;
    private String userName;
    private Message message;
    private KeyBundle userKeyBundle;
    
    public PGPAuthentication(String userName, Message message) {
        this.userName=userName;
        this.message=message;
    }
    
    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOAuthentication#setAuthenticated(boolean)
     */
    public void setAuthenticated(boolean b) {
        authenticated=b;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOAuthentication#isAuthenticated()
     */
    public boolean isAuthenticated() {        
        return authenticated;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOAuthentication#getCredentials()
     */
    public Object getCredentials() {
        return message;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOAuthentication#getDetails()
     */
    public Object getDetails() {
        return userKeyBundle;
    }

    protected void setDetails(KeyBundle kb) {
        userKeyBundle=kb;
    }
    
    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOAuthentication#getPrincipal()
     */
    public Object getPrincipal() {        
        return userName;
    }

}
