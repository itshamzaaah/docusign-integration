package com.example.docusign_integration.servlet;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/docusign/download")
public class DownloadServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Value("${docusign.baseUrl}")
    private String baseUrl;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	    String tokenJson = (String) req.getSession().getAttribute("token");
	    String envelopeId = (String) req.getSession().getAttribute("envelopeId");
	    String accountId = (String) req.getSession().getAttribute("accountId"); // Get from session

	    // Debugging logs
	    System.out.println("Token JSON: " + tokenJson);
	    System.out.println("Envelope ID: " + envelopeId);
	    System.out.println("Account ID: " + accountId);

	    if (tokenJson == null || envelopeId == null || accountId == null) {
	        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	        resp.getWriter().write("Missing token, envelopeId, or accountId in session");
	        return;
	    }

	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode node = mapper.readTree(tokenJson);

	    JsonNode accessTokenNode = node.get("access_token");
	    if (accessTokenNode == null) {
	        resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	        resp.getWriter().write("Invalid token data. 'access_token' missing.");
	        return;
	    }

	    String accessToken = accessTokenNode.asText();

	    ApiClient apiClient = new ApiClient(baseUrl);
	    apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

	    EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
	    try {
	        byte[] documentBytes = envelopesApi.getDocument(accountId, envelopeId, "1");

	        resp.setContentType("application/pdf");
	        resp.setHeader("Content-Disposition", "attachment; filename=signed.pdf");
	        resp.getOutputStream().write(documentBytes);
	    } catch (ApiException e) {
	        e.printStackTrace();
	        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	        resp.getWriter().write("Failed to download document: " + e.getMessage());
	    }
	}
}
