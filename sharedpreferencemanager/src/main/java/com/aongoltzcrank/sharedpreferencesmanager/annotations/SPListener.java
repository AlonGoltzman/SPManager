package com.aongoltzcrank.sharedpreferencesmanager.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a class as an update listener.
 * Each time the preferences are changed, if this class is registered
 * as a listener for the specific key that changed, it will be notified.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPListener {
}
