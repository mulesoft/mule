// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   StringTranslator.java

package com.sun.jbi;


public interface StringTranslator
{

    public abstract String getString(String s);

    public abstract String getString(String s, Object obj);

    public abstract String getString(String s, Object obj, Object obj1);

    public abstract String getString(String s, Object obj, Object obj1, Object obj2);

    public abstract String getString(String s, Object obj, Object obj1, Object obj2, Object obj3);

    public abstract String getString(String s, Object aobj[]);
}
