package com.framework.servlet;

import com.framework.annotation.*;
import com.framework.annotations.API;
import com.framework.annotations.Get;
import com.framework.annotations.Json;
import com.framework.util.ApiResponse;
import com.framework.model.ModelView;
import com.framework.util.ParameterBinder;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrontServlet extends HttpServlet {

    private static class UrlPattern {
        String pattern;
        Pattern regex;
        Method method;
        Object controller;
        List<String> paramNames;
        HttpMethod httpMethod;

        UrlPattern(String pattern, Method method, Object controller, HttpMethod httpMethod) {
            this.pattern = pattern;
            this.method = method;
            this.controller = controller;
            this.httpMethod = httpMethod;
            this.paramNames = new ArrayList<>();
            
            String regexPattern = pattern;
            
            Pattern paramPattern = Pattern.compile("\\{([^}]+)\\}");
            Matcher matcher = paramPattern.matcher(regexPattern);
            
            while (matcher.find()) {
                this.paramNames.add(matcher.group(1));
            }
            
            regexPattern = regexPattern.replaceAll("\\{[^}]+\\}", "([^/]+)");
            regexPattern = "^" + regexPattern + "$";
            
            this.regex = Pattern.compile(regexPattern);
        }

        boolean matches(String url, String method) {
            return this.regex.matcher(url).matches() && this.httpMethod.name().equals(method);
        }

        Map<String, String> extractParams(String url) {
            Map<String, String> params = new HashMap<>();
            Matcher matcher = this.regex.matcher(url);
            
            if (matcher.matches()) {
                for (int i = 0; i < this.paramNames.size(); i++) {
                    params.put(this.paramNames.get(i), matcher.group(i + 1));
                }
            }
            
            return params;
        }
    }

    @Override
    public void init() throws ServletException {
        super.init();
        System.out.println("\n=== Initialisation du Framework (Sprint 8) ===");

        String basePackage = "com.test.controllers";
        List<UrlPattern> urlPatterns = new ArrayList<>();

        try {
            List<Class<?>> classes = getClasses(basePackage);
            System.out.println("Classes trouvées : " + classes.size());
            
            for (Class<?> cls : classes) {
                System.out.println("Classe analysée : " + cls.getName());
                
                if (cls.isAnnotationPresent(Controller.class)) {
                    System.out.println("  -> @Controller détecté");
                    Object instance = cls.getDeclaredConstructor().newInstance();

                    for (Method methodObj : cls.getDeclaredMethods()) {
                        HttpMethod httpMethod = null;
                        String url = null;
                        
                        // Vérifier @GetMapping
                        if (methodObj.isAnnotationPresent(GetMapping.class)) {
                            GetMapping annotation = methodObj.getAnnotation(GetMapping.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.GET;
                        }
                        // Vérifier @PostMapping
                        else if (methodObj.isAnnotationPresent(PostMapping.class)) {
                            PostMapping annotation = methodObj.getAnnotation(PostMapping.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.POST;
                        }
                        // Vérifier @PutMapping
                        else if (methodObj.isAnnotationPresent(PutMapping.class)) {
                            PutMapping annotation = methodObj.getAnnotation(PutMapping.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.PUT;
                        }
                        // Vérifier @DeleteMapping
                        else if (methodObj.isAnnotationPresent(DeleteMapping.class)) {
                            DeleteMapping annotation = methodObj.getAnnotation(DeleteMapping.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.DELETE;
                        }
                        // Vérifier @RequestMapping (générale)
                        else if (methodObj.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping annotation = methodObj.getAnnotation(RequestMapping.class);
                            url = annotation.value();
                            httpMethod = annotation.method();
                        }
                        // Sprint 9: Vérifier les nouvelles annotations @Get, @Post, @Put, @Delete
                        else if (methodObj.isAnnotationPresent(Get.class)) {
                            Get annotation = methodObj.getAnnotation(Get.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.GET;
                        }
                        else if (methodObj.isAnnotationPresent(com.framework.annotations.Post.class)) {
                            com.framework.annotations.Post annotation = methodObj.getAnnotation(com.framework.annotations.Post.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.POST;
                        }
                        else if (methodObj.isAnnotationPresent(com.framework.annotations.Put.class)) {
                            com.framework.annotations.Put annotation = methodObj.getAnnotation(com.framework.annotations.Put.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.PUT;
                        }
                        else if (methodObj.isAnnotationPresent(com.framework.annotations.Delete.class)) {
                            com.framework.annotations.Delete annotation = methodObj.getAnnotation(com.framework.annotations.Delete.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.DELETE;
                        }
                        // Vérifier l'ancienne @Url (compatibilité)
                        else if (methodObj.isAnnotationPresent(Url.class)) {
                            Url annotation = methodObj.getAnnotation(Url.class);
                            url = annotation.value();
                            httpMethod = HttpMethod.GET; // Par défaut GET
                        }
                        
                        if (url != null) {
                            if (!url.startsWith("/")) {
                                url = "/" + url;
                            }
                            
                            UrlPattern urlPattern = new UrlPattern(url, methodObj, instance, httpMethod);
                            urlPatterns.add(urlPattern);

                            Parameter[] params = methodObj.getParameters();
                            StringBuilder paramStr = new StringBuilder();
                            for (Parameter p : params) {
                                paramStr.append(p.getName()).append(", ");
                            }

                            System.out.printf("   ➜ [%s] %-25s → %s.%s(%s)%n",
                                    httpMethod, url, cls.getSimpleName(), methodObj.getName(), 
                                    paramStr.length() > 0 ? paramStr.substring(0, paramStr.length() - 2) : "");
                        }
                    }
                }
            }

            System.out.println("\n=== Patterns enregistrés : " + urlPatterns.size() + " ===\n");

            getServletContext().setAttribute("URL_PATTERNS", urlPatterns);
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
            System.err.println(" ATTENTION : Aucune ressource trouvée pour le package " + packageName);
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
                                System.err.println("   Classe non trouvée : " + className);
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
        processRequest(request, response, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, "PUT");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response, "DELETE");
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response, String httpMethod)
            throws ServletException, IOException {

        // On détecte si la méthode cible est une API REST (Sprint 9)
        boolean isApiRest = false;
        response.setContentType("text/html;charset=UTF-8");

        @SuppressWarnings("unchecked")
        List<UrlPattern> urlPatterns = (List<UrlPattern>) getServletContext().getAttribute("URL_PATTERNS");

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
        System.out.println("Méthode HTTP : " + httpMethod);
        System.out.println("URI complète : " + fullPath);
        System.out.println("Context path : " + contextPath);
        System.out.println("Path extrait : " + path);

        UrlPattern matchedPattern = null;
        Map<String, String> pathParams = new HashMap<>();
        
        if (urlPatterns != null) {
            for (UrlPattern pattern : urlPatterns) {
                if (pattern.matches(path, httpMethod)) {
                    matchedPattern = pattern;
                    pathParams = pattern.extractParams(path);
                    break;
                }
            }
        }

        if (matchedPattern != null) {
            // Sprint 9 : détection API REST
            boolean hasApi = matchedPattern.method.isAnnotationPresent(API.class);
            boolean hasGet = matchedPattern.method.isAnnotationPresent(Get.class);
            boolean hasJson = matchedPattern.method.isAnnotationPresent(Json.class);
            isApiRest = hasApi || hasGet || hasJson;
            System.out.println("✓ Pattern trouvé : " + matchedPattern.pattern + " [" + matchedPattern.httpMethod + "]");
            System.out.println("  Paramètres extraits du path : " + pathParams);

            try {
                for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }

                Parameter[] methodParams = matchedPattern.method.getParameters();
                Object[] args;
                
                System.out.println("  Paramètres de la méthode : " + methodParams.length);
                
                // Sprint 8: Utiliser le ParameterBinder pour le binding automatique avec réflexion
                // Supporte les objets simples, tableaux d'objets et types primitifs
                args = ParameterBinder.bindParameters(methodParams, request);

                Object result = matchedPattern.method.invoke(matchedPattern.controller, args);

                if (isApiRest) {
                    response.setContentType("application/json;charset=UTF-8");
                    String json;
                    if (result != null) {
                        if (result instanceof List<?>) {
                            json = toJson(ApiResponse.success(result));
                        } else {
                            json = toJson(ApiResponse.success(result));
                        }
                    } else {
                        json = toJson(ApiResponse.error(404, "Aucune donnée trouvée"));
                    }
                    out.print(json);
                } else if (result instanceof String str) {
                    out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                    out.println("<h3>Résultat :</h3><pre>" + str + "</pre>");
                    out.println("</body></html>");
                } else if (result instanceof ModelView mv) {
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
                } else {
                    out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                    out.println("<p>Méthode exécutée avec succès (retour : " + (result != null ? result.getClass().getSimpleName() : "null") + ")</p>");
                    out.println("</body></html>");
                }

            } catch (InvocationTargetException e) {
                System.err.println(" Erreur lors de l'invocation de la méthode :");
                e.getCause().printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                out.println("<h2>500 - Erreur lors de l'exécution</h2>");
                out.println("<p><strong>Cause:</strong> " + e.getCause().getMessage() + "</p>");
                out.println("<pre>");
                e.getCause().printStackTrace(out);
                out.println("</pre></body></html>");
            } catch (IllegalArgumentException e) {
                System.err.println(" Erreur IllegalArgumentException - Vérifiez les paramètres :");
                System.err.println("   Message: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
                out.println("<h2>500 - Erreur de paramètres</h2>");
                out.println("<p>Les paramètres ne correspondent pas aux arguments de la méthode.</p>");
                out.println("<p><strong>Message:</strong> " + e.getMessage() + "</p>");
                out.println("<pre>");
                e.printStackTrace(out);
                out.println("</pre></body></html>");
            } catch (Exception e) {
                System.err.println(" Erreur inattendue :");
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
            out.println("<p>Méthode HTTP : <strong>" + httpMethod + "</strong></p>");
            out.println("<p>Patterns disponibles :</p><ul>");
            if (urlPatterns != null) {
                for (UrlPattern pattern : urlPatterns) {
                    out.println("<li>[" + pattern.httpMethod + "] " + pattern.pattern + "</li>");
                }
            }
            out.println("</ul></body></html>");
            System.err.println("✗ Aucun pattern pour : " + httpMethod + " " + path);
        }
    }

    // Utilitaire simple pour convertir en JSON (remplacer par Gson/Jackson si besoin)
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof List<?>) {
                sb.append("[");
                List<?> list = (List<?>) value;
                for (int j = 0; j < list.size(); j++) {
                    sb.append(toJsonObject(list.get(j)));
                    if (j < list.size() - 1) sb.append(",");
                }
                sb.append("]");
            } else if (value instanceof Map<?, ?>) {
                sb.append(toJson((Map<String, Object>) value));
            } else {
                sb.append(toJsonObject(value));
            }
            if (i < map.size() - 1) sb.append(",");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    private String toJsonObject(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + escapeJson((String) obj) + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        if (obj instanceof Map<?, ?>) return toJson((Map<String, Object>) obj);
        // Pour les objets simples, on sérialise les champs publics
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Field[] fields = obj.getClass().getDeclaredFields();
        int i = 0;
        for (Field f : fields) {
            f.setAccessible(true);
            try {
                sb.append("\"").append(f.getName()).append("\":");
                Object v = f.get(obj);
                if (v instanceof String) {
                    sb.append("\"").append(escapeJson((String) v)).append("\"");
                } else if (v instanceof Number || v instanceof Boolean) {
                    sb.append(v);
                } else if (v instanceof List<?>) {
                    sb.append("[");
                    List<?> list = (List<?>) v;
                    for (int j = 0; j < list.size(); j++) {
                        sb.append(toJsonObject(list.get(j)));
                        if (j < list.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                } else if (v instanceof Map<?, ?>) {
                    sb.append(toJson((Map<String, Object>) v));
                } else {
                    sb.append(toJsonObject(v));
                }
            } catch (Exception e) {
                sb.append("null");
            }
            if (i < fields.length - 1) sb.append(",");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}