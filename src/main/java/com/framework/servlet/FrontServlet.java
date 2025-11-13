package com.framework.servlet;

<<<<<<< Updated upstream
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
            System.out.println("🎯 Routes finales enregistrées:");
            routes.keySet().forEach(url -> 
                System.out.println("   " + url + " → " + routes.get(url).getName())
            );
=======
import com.framework.annotation.*;
import com.framework.model.ModelView;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FrontServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("\n=== Initialisation du Framework (Sprint 4 bis) ===");

        String basePackage = "com.test.controllers";
        Map<String, Method> urlMapping = new HashMap<>();
        Map<String, Object> controllerInstances = new HashMap<>();

        try {
            List<Class<?>> classes = getClasses(basePackage);
            System.out.println("Classes trouvées : " + classes.size());
>>>>>>> Stashed changes
            
            for (Class<?> cls : classes) {
                System.out.println("Classe analysée : " + cls.getName());
                
                if (cls.isAnnotationPresent(Controller.class)) {
                    System.out.println("  -> @Controller détecté");
                    Object instance = cls.getDeclaredConstructor().newInstance();

                    for (Method method : cls.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Url.class)) {
                            Url annotation = method.getAnnotation(Url.class);
                            String url = annotation.value();
                            
                            // Normaliser l'URL (enlever le /front si présent dans l'annotation)
                            if (!url.startsWith("/")) {
                                url = "/" + url;
                            }
                            
                            urlMapping.put(url, method);
                            controllerInstances.put(url, instance);

                            System.out.printf("   ➜ URL: %-25s → %s.%s()%n",
                                    url, cls.getSimpleName(), method.getName());
                        }
                    }
                }
            }

            System.out.println("\n=== Mappings enregistrés : " + urlMapping.keySet() + " ===\n");

            getServletContext().setAttribute("URL_MAPPING", urlMapping);
            getServletContext().setAttribute("CONTROLLERS", controllerInstances);
        } catch (Exception e) {
<<<<<<< Updated upstream
            throw new ServletException("Erreur pendant le chargement des annotations", e);
        }
    }

    private void scanPackages(Set<String> packagesToScan) throws Exception {
        for (String packageName : packagesToScan) {
            System.out.println("🔍 Scanning package: " + packageName);
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
            
            System.out.println("🎯 Controller trouvé: " + controllerClass.getSimpleName() + " -> baseUrl: " + baseUrl);
            
            for (Method method : controllerClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Url.class)) {
                    Url urlAnnotation = method.getAnnotation(Url.class);
                    String methodUrl = urlAnnotation.value();
                    
                    // URL finale : /baseUrl/methodUrl
                    String fullUrl = baseUrl + methodUrl;
                    
                    routes.put(fullUrl, method);
                    instances.put(fullUrl, instance);
                    
                    System.out.println("   🧭 " + fullUrl + " → " + method.getName());
=======
            System.err.println("ERREUR lors de l'initialisation :");
            e.printStackTrace();
            throw new ServletException("Erreur d'initialisation du framework", e);
        }
    }

    private List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        
        System.out.println("Recherche dans le package : " + packageName);
        System.out.println("Chemin de recherche : " + path);
        
        Enumeration<URL> resources = classLoader.getResources(path);
    
        if (!resources.hasMoreElements()) {
            System.err.println("⚠️ ATTENTION : Aucune ressource trouvée pour le package " + packageName);
            System.err.println("   Vérifiez que le JAR contient bien ce package");
        }
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();
            
            System.out.println("Ressource trouvée : " + resource);
            System.out.println("Protocole : " + protocol);
    
            if (protocol.equals("file")) {
                File directory = new File(resource.getFile());
                if (directory.exists()) {
                    File[] files = directory.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.getName().endsWith(".class")) {
                                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                                System.out.println("  Chargement : " + className);
                                classes.add(Class.forName(className));
                            }
                        }
                    }
                }
            } else if (protocol.equals("jar")) {
                // Support JAR/WAR
                String jarPath = resource.getPath();
                
                // Extraction du chemin du JAR
                if (jarPath.startsWith("file:")) {
                    jarPath = jarPath.substring(5);
                }
                int separatorIndex = jarPath.indexOf("!");
                if (separatorIndex != -1) {
                    jarPath = jarPath.substring(0, separatorIndex);
                }
                
                // Décodage de l'URL (pour les espaces et caractères spéciaux)
                jarPath = java.net.URLDecoder.decode(jarPath, "UTF-8");
                
                System.out.println("Lecture du JAR : " + jarPath);
                
                try (JarFile jarFile = new JarFile(jarPath)) {
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String entryName = entry.getName();
                        
                        if (entryName.startsWith(path) && entryName.endsWith(".class") && !entry.isDirectory()) {
                            String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                            System.out.println("  Chargement depuis JAR : " + className);
                            try {
                                classes.add(Class.forName(className));
                            } catch (ClassNotFoundException e) {
                                System.err.println("  ⚠️ Classe non trouvée : " + className);
                            }
                        }
                    }
>>>>>>> Stashed changes
                }
            }
        }
    
        return classes;
    }    

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
<<<<<<< Updated upstream
            throws ServletException, IOException {

        String path = request.getRequestURI().substring(request.getContextPath().length());
        
        // Enlève le préfixe /dispatch pour avoir l'URL réelle
        String realPath = path.replaceFirst("^/dispatch", "");
        
        System.out.println("📥 FrontServlet - URL originale: " + path);
        System.out.println("📥 FrontServlet - URL traitée: " + realPath);
        System.out.println("📥 FrontServlet - Routes disponibles: " + routes.keySet());

        response.setContentType("text/plain;charset=UTF-8");

        Method method = routes.get(realPath);

        if (method != null) {
            try {
                Object instance = instances.get(realPath);
                Object result = method.invoke(instance);

                if (result != null) {
                    response.getWriter().println(result.toString());
                } else {
                    response.getWriter().println("✅ Méthode exécutée : " + method.getName());
                }

            } catch (Exception e) {
                response.getWriter().println("❌ Erreur lors de l'exécution : " + e.getMessage());
                e.printStackTrace(response.getWriter());
            }
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                "Aucune méthode trouvée pour " + realPath + "\nRoutes disponibles: " + routes.keySet());
=======
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        @SuppressWarnings("unchecked")
        Map<String, Method> urlMapping = (Map<String, Method>) getServletContext().getAttribute("URL_MAPPING");
        @SuppressWarnings("unchecked")
        Map<String, Object> controllers = (Map<String, Object>) getServletContext().getAttribute("CONTROLLERS");

        String fullPath = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = fullPath.substring(contextPath.length());
        
        // Enlever le préfixe /front si présent
        if (path.startsWith("/front")) {
            path = path.substring(6); // Enlève "/front"
        }
        
        // S'assurer que le path commence par /
        if (!path.startsWith("/") && !path.isEmpty()) {
            path = "/" + path;
        }
        
        PrintWriter out = response.getWriter();
        
        System.out.println("\n=== Requête reçue ===");
        System.out.println("URI complète : " + fullPath);
        System.out.println("Context path : " + contextPath);
        System.out.println("Path extrait : " + path);
        System.out.println("Mappings disponibles : " + (urlMapping != null ? urlMapping.keySet() : "null"));

        if (urlMapping != null && urlMapping.containsKey(path)) {
            Method method = urlMapping.get(path);
            Object controller = controllers.get(path);

            System.out.println("✓ Mapping trouvé : " + method.getDeclaringClass().getSimpleName() + "." + method.getName());

            try {
                Object result = method.invoke(controller);

                if (result instanceof String str) {
                    out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                    out.println("<h3>Résultat :</h3><pre>" + str + "</pre>");
                    out.println("</body></html>");
                } 
                else if (result instanceof ModelView mv) {
                    request.setAttribute("data", mv);
                    String viewPath = mv.getView();
                    
                    // Ajouter .jsp si pas d'extension
                    if (!viewPath.endsWith(".jsp")) {
                        viewPath += ".jsp";
                    }
                    
                    // Ajouter / au début si absent
                    if (!viewPath.startsWith("/")) {
                        viewPath = "/" + viewPath;
                    }
                    
                    System.out.println("Forward vers la vue : " + viewPath);
                    RequestDispatcher dispatcher = request.getRequestDispatcher(viewPath);
                    dispatcher.forward(request, response);
                }
                else {
                    out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                    out.println("<p>Méthode exécutée avec succès (retour : " + (result != null ? result.getClass().getSimpleName() : "null") + ")</p>");
                    out.println("</body></html>");
                }

            } catch (InvocationTargetException e) {
                System.err.println("Erreur lors de l'invocation de la méthode :");
                e.getCause().printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                out.println("<h2>500 - Erreur lors de l'exécution</h2>");
                out.println("<pre>");
                e.getCause().printStackTrace(out);
                out.println("</pre></body></html>");
            } catch (Exception e) {
                System.err.println("Erreur inattendue :");
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                out.println("<h2>500 - Erreur interne</h2>");
                out.println("<pre>");
                e.printStackTrace(out);
                out.println("</pre></body></html>");
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
            out.println("<h2>404 - URL non trouvée</h2>");
            out.println("<p>Path demandé : <strong>" + path + "</strong></p>");
            out.println("<p>Mappings disponibles :</p><ul>");
            if (urlMapping != null) {
                for (String url : urlMapping.keySet()) {
                    out.println("<li>" + url + "</li>");
                }
            }
            out.println("</ul></body></html>");
            System.err.println("✗ Aucun mapping pour : " + path);
>>>>>>> Stashed changes
        }
    }
}