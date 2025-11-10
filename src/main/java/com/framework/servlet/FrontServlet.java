package com.framework.servlet;

import com.framework.annotation.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class FrontServlet extends HttpServlet {
    
    private Map<String, Mapping> urlMappings = new HashMap<>();

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println(" Initialisation Framework...");
        
        try {
            // Scanner ny contrôleurs
            scanControllers("com.test.controllers");
            getServletContext().setAttribute("URL_MAPPINGS", urlMappings);
            System.out.println(" Framework initialisé: " + urlMappings.size() + " URLs mappées");
            
        } catch (Exception e) {
            throw new ServletException("Erreur initialisation framework", e);
        }
    }

    private void scanControllers(String packageName) throws Exception {
        // Manao simulation fotsiny satria tsy mety ny scanner
        // Ampidirina mivantana ny contrôleur
        Class<?> controllerClass = Class.forName("com.test.controllers.TestController");
        
        if (controllerClass.isAnnotationPresent(Controller.class)) {
            Controller controllerAnn = controllerClass.getAnnotation(Controller.class);
            String baseUrl = controllerAnn.value();
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Url.class)) {
                    Url urlAnn = method.getAnnotation(Url.class);
                    String fullUrl = baseUrl + urlAnn.value();
                    
                    urlMappings.put(fullUrl, new Mapping(controllerInstance, method));
                    System.out.println(" " + fullUrl + " → " + method.getName());
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doProcess(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doProcess(request, response);
    }

    private void doProcess(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());
        
        System.out.println("🔍 URL demandée: " + path);
        
        response.setContentType("text/html;charset=UTF-8");
        
        Mapping mapping = urlMappings.get(path);
        
        if (mapping != null) {
            try {
                String result = (String) mapping.method.invoke(mapping.controller);
                
                // Soraty ny valiny
                response.getWriter().println("<html><body>");
                response.getWriter().println("<h1>" + result + "</h1>");
                response.getWriter().println("<p><strong>URL:</strong> " + path + "</p>");
                response.getWriter().println("<p><a href='" + contextPath + "/front'>Accueil</a> | ");
                response.getWriter().println("<a href='" + contextPath + "/front/home'>Home</a> | ");
                response.getWriter().println("<a href='" + contextPath + "/front/about'>About</a></p>");
                response.getWriter().println("</body></html>");
                
            } catch (Exception e) {
                response.sendError(500, "Erreur: " + e.getMessage());
            }
        } else {
            // Afficher liste des URLs disponibles
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h2>URLs Disponibles:</h2>");
            response.getWriter().println("<ul>");
            for (String url : urlMappings.keySet()) {
                response.getWriter().println("<li><a href='" + contextPath + url + "'>" + url + "</a></li>");
            }
            response.getWriter().println("</ul>");
            response.getWriter().println("</body></html>");
        }
    }
    
    // Classe interne pour stocker les mappings
    private static class Mapping {
        Object controller;
        Method method;
        
        Mapping(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
        }
    }
}