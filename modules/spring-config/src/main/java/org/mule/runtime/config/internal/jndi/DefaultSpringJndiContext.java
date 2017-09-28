/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.jndi;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.naming.Binding;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NotContextException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;

/**
 * TODO
 */

/**
 * A simple spring based JNDI context which is mutable
 * <p/>
 * Borrowed from the XBean (xbean.codehaus.org) project. Thanks guys!
 */
public class DefaultSpringJndiContext implements Context, Serializable {

  private static final long serialVersionUID = -5754338187296859149L;
  protected static final NameParser nameParser = new DefaultNameParser();

  private boolean freeze = false;

  protected final Hashtable environment; // environment for this context
  protected final Map bindings; // bindings at my level
  protected final Map treeBindings; // all bindings under me

  private boolean frozen = false;
  private String nameInNamespace = "";
  public static final String SEPARATOR = "/";

  public DefaultSpringJndiContext() {
    environment = new Hashtable();
    bindings = new HashMap();
    treeBindings = new HashMap();
  }

  public DefaultSpringJndiContext(Hashtable env) {
    if (env == null) {
      this.environment = new Hashtable();
    } else {
      this.environment = new Hashtable(env);
    }
    this.bindings = new HashMap();
    this.treeBindings = new HashMap();
  }

  public DefaultSpringJndiContext(Hashtable environment, Map bindings) {
    if (environment == null) {
      this.environment = new Hashtable();
    } else {
      this.environment = new Hashtable(environment);
    }
    this.bindings = bindings;
    treeBindings = new HashMap();
    frozen = true;
  }

  public DefaultSpringJndiContext(Hashtable environment, Map bindings, String nameInNamespace) {
    this(environment, bindings);
    this.nameInNamespace = nameInNamespace;
  }

  protected DefaultSpringJndiContext(DefaultSpringJndiContext clone, Hashtable env) {
    this.bindings = clone.bindings;
    this.treeBindings = clone.treeBindings;
    this.environment = new Hashtable(env);
  }

  protected DefaultSpringJndiContext(DefaultSpringJndiContext clone, Hashtable env, String nameInNamespace) {
    this(clone, env);
    this.nameInNamespace = nameInNamespace;
  }

  public Object addToEnvironment(String propName, Object propVal) throws NamingException {
    return environment.put(propName, propVal);
  }

  public Hashtable getEnvironment() throws NamingException {
    return (Hashtable) environment.clone();
  }

  public Object removeFromEnvironment(String propName) throws NamingException {
    return environment.remove(propName);
  }

  public Object lookup(String name) throws NamingException {
    if (name.length() == 0) {
      return this;
    }
    Object result = treeBindings.get(name);
    if (result == null) {
      result = bindings.get(name);
    }
    if (result == null) {
      int pos = name.indexOf(':');
      if (pos > 0) {
        String scheme = name.substring(0, pos);
        Context ctx = NamingManager.getURLContext(scheme, environment);
        if (ctx == null) {
          throw new NamingException("scheme " + scheme + " not recognized");
        }
        return ctx.lookup(name);
      } else {
        // Split out the first name of the path
        // and look for it in the bindings map.
        CompositeName path = new CompositeName(name);

        if (path.size() == 0) {
          return this;
        } else {
          String first = path.get(0);
          Object obj = bindings.get(first);
          if (obj == null) {
            throw new NameNotFoundException(name);
          } else if (obj instanceof Context && path.size() > 1) {
            Context subContext = (Context) obj;
            obj = subContext.lookup(path.getSuffix(1));
          }
          return obj;
        }
      }
    }
    if (result instanceof LinkRef) {
      LinkRef ref = (LinkRef) result;
      result = lookup(ref.getLinkName());
    }
    if (result instanceof Reference) {
      try {
        result = NamingManager.getObjectInstance(result, null, null, this.environment);
      } catch (NamingException e) {
        throw e;
      } catch (Exception e) {
        throw (NamingException) new NamingException("could not look up : " + name).initCause(e);
      }
    }
    if (result instanceof DefaultSpringJndiContext) {
      String prefix = getNameInNamespace();
      if (prefix.length() > 0) {
        prefix = prefix + SEPARATOR;
      }
      result = new DefaultSpringJndiContext((DefaultSpringJndiContext) result, environment, prefix + name);
    }
    return result;
  }

  public Object lookup(Name name) throws NamingException {
    return lookup(name.toString());
  }

  public Object lookupLink(String name) throws NamingException {
    return lookup(name);
  }

  public Name composeName(Name name, Name prefix) throws NamingException {
    Name result = (Name) prefix.clone();
    result.addAll(name);
    return result;
  }

  public String composeName(String name, String prefix) throws NamingException {
    CompositeName result = new CompositeName(prefix);
    result.addAll(new CompositeName(name));
    return result.toString();
  }

  public NamingEnumeration list(String name) throws NamingException {
    Object o = lookup(name);
    if (o == this) {
      return new DefaultSpringJndiContext.ListEnumeration();
    } else if (o instanceof Context) {
      return ((Context) o).list("");
    } else {
      throw new NotContextException();
    }
  }

  public NamingEnumeration listBindings(String name) throws NamingException {
    Object o = lookup(name);
    if (o == this) {
      return new DefaultSpringJndiContext.ListBindingEnumeration();
    } else if (o instanceof Context) {
      return ((Context) o).listBindings("");
    } else {
      throw new NotContextException();
    }
  }

  public Object lookupLink(Name name) throws NamingException {
    return lookupLink(name.toString());
  }

  public NamingEnumeration list(Name name) throws NamingException {
    return list(name.toString());
  }

  public NamingEnumeration listBindings(Name name) throws NamingException {
    return listBindings(name.toString());
  }

  public void bind(Name name, Object value) throws NamingException {
    bind(name.toString(), value);
  }

  public void bind(String name, Object value) throws NamingException {
    checkFrozen();
    internalBind(name, value);
  }

  public void close() throws NamingException {
    // ignore
  }

  public Context createSubcontext(Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public Context createSubcontext(String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void destroySubcontext(Name name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public void destroySubcontext(String name) throws NamingException {
    throw new OperationNotSupportedException();
  }

  public String getNameInNamespace() throws NamingException {
    return nameInNamespace;
  }

  public NameParser getNameParser(Name name) throws NamingException {
    return nameParser;
  }

  public NameParser getNameParser(String name) throws NamingException {
    return nameParser;
  }

  public void rebind(Name name, Object value) throws NamingException {
    rebind(name.toString(), value);
  }

  public void rebind(String name, Object value) throws NamingException {
    checkFrozen();
    internalBind(name, value, true);
  }

  public void rename(Name oldName, Name newName) throws NamingException {
    checkFrozen();
    Object value = lookup(oldName);
    unbind(oldName);
    bind(newName, value);
  }

  public void rename(String oldName, String newName) throws NamingException {
    Object value = lookup(oldName);
    unbind(oldName);
    bind(newName, value);
  }

  public void unbind(Name name) throws NamingException {
    unbind(name.toString());
  }

  public void unbind(String name) throws NamingException {
    checkFrozen();
    internalBind(name, null, true);
  }

  private abstract class AbstractLocalNamingEnumeration implements NamingEnumeration {

    private Iterator i = bindings.entrySet().iterator();

    public boolean hasMore() throws NamingException {
      return i.hasNext();
    }

    public boolean hasMoreElements() {
      return i.hasNext();
    }

    protected Map.Entry getNext() {
      return (Map.Entry) i.next();
    }

    public void close() throws NamingException {}
  }

  private class ListEnumeration extends AbstractLocalNamingEnumeration {

    public Object next() throws NamingException {
      return nextElement();
    }

    public Object nextElement() {
      Map.Entry entry = getNext();
      return new NameClassPair((String) entry.getKey(), entry.getValue().getClass().getName());
    }
  }

  private class ListBindingEnumeration extends AbstractLocalNamingEnumeration {

    public Object next() throws NamingException {
      return nextElement();
    }

    public Object nextElement() {
      Map.Entry entry = getNext();
      return new Binding((String) entry.getKey(), entry.getValue());
    }
  }

  public Map getEntries() {
    return new HashMap(bindings);
  }

  public void setEntries(Map entries) throws NamingException {
    if (entries != null) {
      for (Iterator iter = entries.entrySet().iterator(); iter.hasNext();) {
        Map.Entry entry = (Map.Entry) iter.next();
        String name = (String) entry.getKey();
        Object value = entry.getValue();
        internalBind(name, value);
      }
    }
  }

  public boolean isFreeze() {
    return freeze;
  }

  public void setFreeze(boolean freeze) {
    this.freeze = freeze;
  }

  /**
   * internalBind is intended for use only during setup or possibly by suitably synchronized superclasses. It binds every possible
   * lookup into a map in each context. To do this, each context strips off one name segment and if necessary creates a new
   * context for it. Then it asks that context to bind the remaining name. It returns a map containing all the bindings from the
   * next context, plus the context it just created (if it in fact created it). (the names are suitably extended by the segment
   * originally lopped off).
   *
   * @param name
   * @param value
   * @throws javax.naming.NamingException
   */
  protected Map internalBind(String name, Object value) throws NamingException {
    return internalBind(name, value, false);

  }

  protected Map internalBind(String name, Object value, boolean allowRebind) throws NamingException {
    if (name == null || name.length() == 0) {
      throw new NamingException("Invalid Name " + name);
    }
    if (frozen) {
      throw new NamingException("Read only");
    }

    Map newBindings = new HashMap();
    int pos = name.indexOf('/');
    if (pos == -1) {
      Object oldValue = treeBindings.put(name, value);
      if (!allowRebind && oldValue != null) {
        throw new NamingException("Something already bound at " + name);
      }
      bindings.put(name, value);
      newBindings.put(name, value);
    } else {
      String segment = name.substring(0, pos);

      if (segment == null || segment.length() == 0) {
        throw new NamingException("Invalid segment " + segment);
      }
      Object o = treeBindings.get(segment);
      if (o == null) {
        o = newContext();
        treeBindings.put(segment, o);
        bindings.put(segment, o);
        newBindings.put(segment, o);
      } else if (!(o instanceof DefaultSpringJndiContext)) {
        throw new NamingException("Something already bound where a subcontext should go");
      }
      DefaultSpringJndiContext defaultContext = (DefaultSpringJndiContext) o;
      String remainder = name.substring(pos + 1);
      Map subBindings = defaultContext.internalBind(remainder, value, allowRebind);
      for (Iterator iterator = subBindings.entrySet().iterator(); iterator.hasNext();) {
        Map.Entry entry = (Map.Entry) iterator.next();
        String subName = segment + "/" + (String) entry.getKey();
        Object bound = entry.getValue();
        treeBindings.put(subName, bound);
        newBindings.put(subName, bound);
      }
    }
    return newBindings;
  }

  protected void checkFrozen() throws OperationNotSupportedException {
    if (isFreeze()) {
      throw new OperationNotSupportedException("JNDI context is frozen!");
    }
  }

  protected DefaultSpringJndiContext newContext() {
    return new DefaultSpringJndiContext();
  }

}

