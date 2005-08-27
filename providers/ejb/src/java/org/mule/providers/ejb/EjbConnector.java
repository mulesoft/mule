/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.providers.rmi.RmiConnector;
import org.mule.umo.lifecycle.InitialisationException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;
/*
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:pnirvin@hotmail.com">P.Oikari</a>
 * @version $Revision$
 */
public class EjbConnector extends RmiConnector
{
  private String jndiInitialFactory;

  private String jndiUrlPkgPrefixes;

  private String jndiProviderUrl;

  private Context jndiContext;

  public String getProtocol()
  {
    return "ejb";
  }

  protected void initJndiContext() throws NamingException, InitialisationException
  {
    if (null == jndiContext)
    {
      Hashtable props = new Hashtable();

      if (null != jndiInitialFactory)
        props.put(Context.INITIAL_CONTEXT_FACTORY, jndiInitialFactory);

      if (jndiProviderUrl != null)
        props.put(Context.PROVIDER_URL, jndiProviderUrl);

      if (jndiUrlPkgPrefixes != null)
        props.put(Context.URL_PKG_PREFIXES, jndiUrlPkgPrefixes);

      jndiContext = new InitialContext(props);
    }
  }

  public Context getJndiContext(String jndiProviderUrl) throws InitialisationException
  {
    try
    {
      setJndiProviderUrl(jndiProviderUrl);

      initJndiContext();
    }
    catch (Exception e)
    {
      throw new InitialisationException(new Message(Messages.FAILED_TO_CREATE_X, "EJB Connector"), e, this);
    }

    return jndiContext;
  }

  public void setJndiContext(Context jndiContext)
  {
    this.jndiContext = jndiContext;
  }

  public void setJndiInitialFactory(String jndiInitialFactory)
  {
    this.jndiInitialFactory = jndiInitialFactory;
  }

  public String getJndiInitialFactory()
  {
    return jndiInitialFactory;
  }

  public void setJndiUrlPkgPrefixes(String jndiUrlPkgPrefixes)
  {
    this.jndiUrlPkgPrefixes = jndiUrlPkgPrefixes;
  }

  public String getJndiUrlPkgPrefixes()
  {
    return jndiUrlPkgPrefixes;
  }

  public String getJndiProviderUrl()
  {
    return jndiProviderUrl;
  }

  public void setJndiProviderUrl(String jndiProviderUrl)
  {
    this.jndiProviderUrl = jndiProviderUrl;
  }
}
