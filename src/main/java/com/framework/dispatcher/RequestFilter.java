package com.framework.dispatcher;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.File;
import java.io.IOException;

public class RequestFilter implements Filter {

    private ServletContext context;

    @Override
    public void init(FilterConfig filterConfig) {
        this.context = filterConfig.getServletContext();
        System.out.println("✅ RequestFilter initialisé");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        System.out.println("🔍 Filtre - URL demandée: " + path);

        // Vérifie si la ressource existe physiquement
        String realPath = context.getRealPath(path);
        File resource = new File(realPath != null ? realPath : "");

        if (resource.exists() && !resource.isDirectory()) {
            System.out.println("📁 Ressource statique trouvée: " + path);
            chain.doFilter(request, response);
        } else if (path.startsWith("/front") || path.startsWith("/admin") || path.startsWith("/api")) {
            System.out.println("🚀 URL framework détectée, redirection vers FrontServlet: " + path);
            // Redirige vers notre point d'entrée unique
            req.getRequestDispatcher("/dispatch" + path).forward(req, res);
        } else {
            System.out.println("❌ URL non trouvée: " + path);
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Ressource non trouvée : " + path);
        }
    }

    @Override
    public void destroy() {
    }
}