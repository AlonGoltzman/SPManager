package com.sharedpreferencesmanager.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a specific method as the target of any updates.
 * This must be used in unison with {@link SPListener} and must receive one string parameter and one object parameter.
 * TODO: Change object parameter to SharedPreferenceObject parameter.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SPUpdateTarget {

    /**
     * Specifies which keys this listener is interested in.
     */
    String[] keys() default "*";
}
