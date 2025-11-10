package com.framework.dispatcher;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class RequestFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        System.out.println(" Filter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        System.out.println(" Filter - Path: " + path);
        
        // Raha URL manomboka amin'ny /front, alefa any amin'ny FrontServlet
        if (path.startsWith("/front")) {
            System.out.println("➡️ Redirect to FrontServlet: " + path);
            req.getRequestDispatcher(path).forward(request, response);
        } else {
            // Raha tsy /front, avela handeha
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }
}