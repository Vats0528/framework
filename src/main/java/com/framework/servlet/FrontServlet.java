package com.framework.servlet;

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
        System.out.println("\n=== Initialisation du Framework (Sprint 5) ===");

        String basePackage = "com.test.controllers";
        Map<String, Method> urlMapping = new HashMap<>();
        Map<String, Object> controllerInstances = new HashMap<>();

        try {
            List<Class<?>> classes = getClasses(basePackage);
            System.out.println("Classes trouvées : " + classes.size());
            
            for (Class<?> cls : classes) {
                System.out.println("Classe analysée : " + cls.getName());
                
                if (cls.isAnnotationPresent(Controller.class)) {
                    System.out.println("  -> @Controller détecté");
                    Object instance = cls.getDeclaredConstructor().newInstance();

                    for (Method method : cls.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(Url.class)) {
                            Url annotation = method.getAnnotation(Url.class);
                            String url = annotation.value();
                            
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
                String jarPath = resource.getPath();
                
                if (jarPath.startsWith("file:")) {
                    jarPath = jarPath.substring(5);
                }
                int separatorIndex = jarPath.indexOf("!");
                if (separatorIndex != -1) {
                    jarPath = jarPath.substring(0, separatorIndex);
                }
                
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
                }
            }
        }
    
        return classes;
    }    

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
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
        
        if (path.startsWith("/front")) {
            path = path.substring(6);
        }
        
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
                    // Ajouter tous les attributs du ModelView à la requête
                    for (Map.Entry<String, Object> entry : mv.getAttributes().entrySet()) {
                        request.setAttribute(entry.getKey(), entry.getValue());
                    }
                    
                    String viewPath = mv.getView();
                    
                    if (!viewPath.endsWith(".jsp")) {
                        viewPath += ".jsp";
                    }
                    
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
        }
    }
}