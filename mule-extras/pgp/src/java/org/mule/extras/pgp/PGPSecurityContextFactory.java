/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 21-mar-2005
 *
 */
package org.mule.extras.pgp;

import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;

/**
 * @author ariva
 *
 */
public class PGPSecurityContextFactory implements UMOSecurityContextFactory {

    /* (non-Javadoc)
     * @see org.mule.umo.security.UMOSecurityContextFactory#create(org.mule.umo.security.UMOAuthentication)
     */
    public UMOSecurityContext create(UMOAuthentication authentication) {
        return new PGPSecurityContext((PGPAuthentication)authentication);
    }

}
