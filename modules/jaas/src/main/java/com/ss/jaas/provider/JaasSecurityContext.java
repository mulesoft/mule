package com.ss.jaas.provider;

import org.mule.impl.security.MuleAuthentication;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;

/**
 * 
 * @author Marie.Rizzo
 *
 */
public class JaasSecurityContext implements UMOSecurityContext {

	MuleAuthentication authentication;
	
	public JaasSecurityContext(MuleAuthentication authentication)
    {
        this.authentication = authentication;
    }
	
	public UMOAuthentication getAuthentication() {
	    return authentication;
	}

	public void setAuthentication(UMOAuthentication authentication) {
		this.authentication = (MuleAuthentication) authentication;
	}
}