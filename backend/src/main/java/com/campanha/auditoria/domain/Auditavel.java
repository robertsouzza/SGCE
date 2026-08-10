package com.campanha.auditoria.domain;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca um método de use case cujas invocações devem ser gravadas em
 * logs_auditoria via AuditoriaAspect.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditavel {
    /** Nome legível da ação (ex: "cadastrar_partido"). */
    String acao();
    /** Nome da entidade afetada (ex: "Partido"). */
    String entidade();
}
