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
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI().substring(req.getContextPath().length());

        // Vérifie si la ressource existe physiquement dans le dossier du projet
        String realPath = context.getRealPath(path);
        File resource = new File(realPath != null ? realPath : "");

        if (resource.exists() && !resource.isDirectory()) {
            // Si la ressource existe, Tomcat la sert normalement
            chain.doFilter(request, response);
        } else if (path.startsWith("/front")) {
            // Si l'URL commence par /front, on envoie vers la FrontServlet
            req.getRequestDispatcher(path).forward(req, res);
        } else {
            // Sinon, on renvoie une erreur 404
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Ressource non trouvée : " + path);
        }
    }

    @Override
    public void destroy() {
    }
}
