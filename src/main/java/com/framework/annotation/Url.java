package com.framework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
<<<<<<< Updated upstream
import java.lang.annotation.ElementType;
=======
>>>>>>> Stashed changes

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Url {
    String value(); // représente l'URL (relative au contrôleur)
}