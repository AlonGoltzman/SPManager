package com.aongoltzcrank.sharedpreferencesmanager.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a class as the shared preference manager instance.
 * An annotated class must contain a private default constructor as well as a
 * static method to receive an instance of the class, i.e. a singleton.
 * TODO: In the later version, add option to mark override methods.
 * TODO: Link this and other annotations to avoid "leaks" or awful, untraceable bugs.
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface SPManager {

}
