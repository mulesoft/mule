/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.acegi;

import java.util.HashMap;
import java.util.Map;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.context.SecurityContextImpl;
import org.mule.MuleManager;
import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.impl.security.MuleCredentials;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.UMOMessage;

public class AuthenticationWithComponentSecurityAsynch extends FunctionalTestCase 
{       
        // Clear the security context after each test.
        public void teardown() {
            SecurityContextHolder.setContext(new SecurityContextImpl());
        }
        
        public void testCaseGoodAuthenticationGoodAuthorisation() throws Exception{  
            MuleClient client = new MuleClient();       
            Map props = new HashMap();
            
            UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
            String header=  MuleCredentials.createHeader("marie", "marie", "PBE", strategy);
            props.put(MuleProperties.MULE_USER_PROPERTY, header);
            client.dispatch("vm://test", "Marie",props);
            UMOMessage m = client.receive("vm://output", 3000);
            assertNotNull(m);
            assertEquals((String) m.getPayload(),"Marie");
        }
        
        public void testCaseGoodAuthenticationBadAuthorisation() throws Exception{  
            MuleClient client = new MuleClient();       
            Map props = new HashMap();
            
            UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
            String header=  MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
            props.put(MuleProperties.MULE_USER_PROPERTY, header);
            client.dispatch("vm://test", "Marie",props);
            UMOMessage m = client.receive("vm://output", 3000);
            assertNull(m);
       }
        
        public void testCaseBadAuthentication() throws Exception{  
            MuleClient client = new MuleClient();       
            Map props = new HashMap();
            
            UMOEncryptionStrategy strategy = MuleManager.getInstance().getSecurityManager().getEncryptionStrategy("PBE");
            String header=  MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
            props.put(MuleProperties.MULE_USER_PROPERTY, header);
            client.dispatch("vm://test", "USD,MTL",props);
            UMOMessage m = client.receive("vm://output", 3000);
            assertNull(m);
        }
                            
        protected String getConfigResources() {
            return "test-mule-component-security-asynchronous-config.xml";
        } 
    }