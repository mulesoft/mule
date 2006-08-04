package com.ss.jaas.provider;

import org.mule.impl.security.MuleAuthentication;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;


/**
 * 
 * @author Marie.Rizzo
 *
 */
public class JaasSecurityContextFactory implements UMOSecurityContextFactory
{
    /**
     * Creates the Jaas Security Context
     */
	public UMOSecurityContext create(UMOAuthentication authentication)
    {
        return new JaasSecurityContext((MuleAuthentication) authentication);
    }

}