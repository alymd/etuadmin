package com.eduadmin.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception ex) {
        log.error("Une erreur inattendue est survenue : ", ex);
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.addObject("titre", "Erreur Interne");
        modelAndView.addObject("message", "Une erreur inattendue est survenue sur le serveur.");
        modelAndView.addObject("details", ex.getMessage());
        
        return modelAndView;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Requete invalide : {}", ex.getMessage());
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.addObject("titre", "Requête Invalide");
        modelAndView.addObject("message", "Les données soumises sont incorrectes ou incomplètes.");
        modelAndView.addObject("details", ex.getMessage());
        
        return modelAndView;
    }
}
