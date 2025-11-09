package com.framework.servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import com.framework.annotation.Url;
import com.framework.annotation.Controller;

public class FrontServlet extends HttpServlet {

    private Map<String, Method> routes = new HashMap<>();
    private Map<String, Object> instances = new HashMap<>();

    @Override
    public void init() throws ServletException {
        try {
            // Packages à scanner
            Set<String> packagesToScan = new HashSet<>();
            packagesToScan.add("com.test.controllers");
            
            // Scanner les packages
            scanPackages(packagesToScan);
            
            // Afficher toutes les routes enregistrées
            System.out.println("Routes finales enregistrées:");
            routes.keySet().forEach(url -> 
                System.out.println("   " + url + " → " + routes.get(url).getName())
            );
            
        } catch (Exception e) {
            throw new ServletException("Erreur pendant le chargement des annotations", e);
        }
    }

    private void scanPackages(Set<String> packagesToScan) throws Exception {
        for (String packageName : packagesToScan) {
            System.out.println("Scanning package: " + packageName);
            scanPackageManually(packageName);
        }
    }

    private void scanPackageManually(String packageName) throws Exception {
        // Liste manuelle des classes dans le package
        if ("com.test.controllers".equals(packageName)) {
            registerController("com.test.controllers.TestController");
            registerController("com.test.controllers.AdminController");
        }
    }

    private void registerController(String className) throws Exception {
        Class<?> controllerClass = Class.forName(className);
        
        if (controllerClass.isAnnotationPresent(Controller.class)) {
            Controller controllerAnnotation = controllerClass.getAnnotation(Controller.class);
            String baseUrl = controllerAnnotation.value();
            
            Object instance = controllerClass.getDeclaredConstructor().newInstance();
            
            System.out.println("Controller trouvé: " + controllerClass.getSimpleName() + " -> baseUrl: " + baseUrl);
            
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Url.class)) {
                    Url urlAnnotation = method.getAnnotation(Url.class);
                    String methodUrl = urlAnnotation.value();
                    
                    // URL finale : /baseUrl/methodUrl
                    String fullUrl = baseUrl + methodUrl;
                    
                    routes.put(fullUrl, method);
                    instances.put(fullUrl, instance);
                    
                    System.out.println("    " + fullUrl + " → " + method.getName());
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Enlève le préfixe /dispatch pour avoir l'URL réelle
        String realPath = path.replaceFirst("^/dispatch", "");
        
        System.out.println("FrontServlet - URL originale: " + path);
        System.out.println("FrontServlet - URL traitée: " + realPath);
        System.out.println("FrontServlet - Routes disponibles: " + routes.keySet());

        response.setContentType("text/plain;charset=UTF-8");

        Method method = routes.get(realPath);

        if (method != null) {
            try {
                Object instance = instances.get(realPath);
                Object result = method.invoke(instance);

                if (result != null) {
                    response.getWriter().println(result.toString());
                } else {
                    response.getWriter().println(" Méthode exécutée : " + method.getName());
                }

            } catch (Exception e) {
                response.getWriter().println(" Erreur lors de l'exécution : " + e.getMessage());
                e.printStackTrace(response.getWriter());
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                "Aucune méthode trouvée pour " + realPath + "\nRoutes disponibles: " + routes.keySet());
        }
    }
}