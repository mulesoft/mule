package org.mule.util.scan.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.ANNOTATION_TYPE)
public @interface Meta
{
}