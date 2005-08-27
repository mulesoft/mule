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

import org.mule.providers.rmi.RmiMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;

/*
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:pnirvin@hotmail.com">P.Oikari</a>
 * @version $Revision$
 */
public class EjbMessageAdapter extends RmiMessageAdapter
{
  public EjbMessageAdapter(Object message) throws MessageTypeNotSupportedException
  {
    super(message);
  }
}
