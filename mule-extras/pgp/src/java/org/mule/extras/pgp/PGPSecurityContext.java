/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 21-mar-2005
 *
 */
package org.mule.extras.pgp;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;

/**
 * @author ariva
 *
 */
public class PGPSecurityContext implements UMOSecurityContext {

    PGPAuthentication authentication;
    
    public PGPSecurityContext(PGPAuthentication authentication) {
        this.authentication=authentication;
    }
    
    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOSecurityContext#setAuthentication(org.mule.umo.security.UMOAuthentication)
     */
    public void setAuthentication(UMOAuthentication authentication) {
       this.authentication=(PGPAuthentication)authentication;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOSecurityContext#getAuthentication()
     */
    public UMOAuthentication getAuthentication() {
        return authentication;
    }

}
