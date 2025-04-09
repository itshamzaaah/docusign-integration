package com.example.docusign_integration.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/docusign/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Value("${docusign.clientId}")
    private String clientId;

    @Value("${docusign.redirectUri}")
    private String redirectUri;

    @Value("${docusign.authUrl}")
    private String authUrl;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = authUrl + "?response_type=code&scope=signature" +
                "&client_id=" + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        resp.sendRedirect(url);
    }
}
