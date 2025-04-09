package com.example.docusign_integration.servlet;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/docusign/callback")
public class CallbackServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Value("${docusign.clientId}")
    private String clientId;

    @Value("${docusign.clientSecret}")
    private String clientSecret;

    @Value("${docusign.redirectUri}")
    private String redirectUri;

    @Value("${docusign.tokenUrl}")
    private String tokenUrl;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String code = req.getParameter("code");

        HttpClient client = HttpClient.newHttpClient();
        String form = "grant_type=authorization_code" +
                      "&code=" + code +
                      "&client_id=" + clientId +
                      "&client_secret=" + clientSecret +
                      "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .headers("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            req.getSession().setAttribute("token", response.body());
            resp.getWriter().write("Logged in! You can now send a document: /docusign/send");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Good practice
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Request was interrupted.");
        }
    }

}
