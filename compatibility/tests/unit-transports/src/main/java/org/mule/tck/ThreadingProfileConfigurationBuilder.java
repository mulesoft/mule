/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import static org.mule.compatibility.core.api.config.MuleEndpointProperties.OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE;
import static org.mule.compatibility.core.api.config.MuleEndpointProperties.OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE;
import static org.mule.compatibility.core.api.config.MuleEndpointProperties.OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE;
import static org.mule.compatibility.core.api.config.MuleEndpointProperties.OBJECT_DEFAULT_THREADING_PROFILE;

import org.mule.compatibility.core.api.config.ThreadingProfile;
import org.mule.compatibility.core.config.ChainedThreadingProfile;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.builders.DefaultsConfigurationBuilder;


public class ThreadingProfileConfigurationBuilder extends DefaultsConfigurationBuilder {

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    configureThreadingProfiles(muleContext);
  }

  protected void configureThreadingProfiles(MuleContext muleContext) throws RegistrationException {
    ThreadingProfile defaultThreadingProfile = new ChainedThreadingProfile();
    registerObject(OBJECT_DEFAULT_THREADING_PROFILE, defaultThreadingProfile, muleContext);

    registerObject(OBJECT_DEFAULT_MESSAGE_RECEIVER_THREADING_PROFILE, new ChainedThreadingProfile(defaultThreadingProfile),
                   muleContext);
    registerObject(OBJECT_DEFAULT_MESSAGE_REQUESTER_THREADING_PROFILE, new ChainedThreadingProfile(defaultThreadingProfile),
                   muleContext);
    registerObject(OBJECT_DEFAULT_MESSAGE_DISPATCHER_THREADING_PROFILE, new ChainedThreadingProfile(defaultThreadingProfile),
                   muleContext);
  }

}
