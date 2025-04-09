package com.example.docusign_integration.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;

import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/docusign/send")
public class SendDocumentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // Inject the DocuSign base URL property from application.properties
    @Value("${docusign.baseUrl}")
    private String baseUrl;

    // The user info endpoint for the DocuSign demo environment.
    private final String userInfoEndpoint = "https://account-d.docusign.com/oauth/userinfo";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Retrieve the OAuth token from the session
        String tokenJson = (String) req.getSession().getAttribute("token");
        if (tokenJson == null) {
            resp.getWriter().write("Error: OAuth token not found in session. Please complete the authentication flow.");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try {
            node = mapper.readTree(tokenJson);
        } catch (Exception e) {
            resp.getWriter().write("Error parsing token JSON: " + e.getMessage());
            return;
        }

        // Get the access token from the JSON token
        JsonNode accessTokenNode = node.get("access_token");
        if (accessTokenNode == null) {
            resp.getWriter().write("Error: access_token not found in token JSON. Token JSON: " + tokenJson);
            return;
        }
        String accessToken = accessTokenNode.asText();

        // Retrieve account_id from the user info endpoint
        String accountId = retrieveAccountIdFromUserInfo(accessToken, mapper);
        if (accountId == null) {
            resp.getWriter().write("Error: Unable to retrieve account_id from user info endpoint.");
            return;
        }
        
        req.getSession().setAttribute("accountId", accountId); // <-- Save for later use


        // Create the DocuSign API client with the base URL (value injected via @Value)
        ApiClient apiClient = new ApiClient(baseUrl);
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);

        // Load the document from the classpath
        InputStream is = getClass().getResourceAsStream("/Docusign.pdf");
        if (is == null) {
            resp.getWriter().write("Error: PDF document not found in the classpath.");
            return;
        }
        byte[] fileBytes = is.readAllBytes();
        String base64File = Base64.getEncoder().encodeToString(fileBytes);

        // Create a document for the envelope
        Document doc = new Document();
        doc.setDocumentBase64(base64File);
        doc.setName("Agreement.pdf");
        doc.setFileExtension("pdf");
        doc.setDocumentId("1");

        // Setup the signature tab (adjust coordinates if needed)
        SignHere signHere = new SignHere()
                .documentId("1")
                .pageNumber("1")
                .recipientId("1")
                .xPosition("100")
                .yPosition("150");

        // Setup the signer details
        Signer signer = new Signer()
                .email("maratib.hamza@gmail.com")  // Replace with the actual signer's email
                .name("DocuSign Integration")
                .recipientId("1")
                .routingOrder("1")
                .tabs(new Tabs().signHereTabs(List.of(signHere)));

        // Create the envelope definition and set it to sent status to immediately send
        EnvelopeDefinition envelopeDefinition = new EnvelopeDefinition();
        envelopeDefinition.setEmailSubject("Please sign this PDF");
        envelopeDefinition.setDocuments(List.of(doc));
        envelopeDefinition.setRecipients(new Recipients().signers(List.of(signer)));
        envelopeDefinition.setStatus("sent");

        // Send the envelope using DocuSign's EnvelopesApi
        EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
        try {
            EnvelopeSummary result = envelopesApi.createEnvelope(accountId, envelopeDefinition);
            req.getSession().setAttribute("envelopeId", result.getEnvelopeId());
            resp.getWriter().write("Envelope sent successfully! Envelope ID: " + result.getEnvelopeId());
        } catch (com.docusign.esign.client.ApiException e) {
            e.printStackTrace();
            resp.getWriter().write("Failed to send envelope: " + e.getMessage());
        }
    }

    /**
     * Retrieves the account_id by making a call to the DocuSign user info endpoint.
     *
     * @param accessToken the access token from OAuth
     * @param mapper an ObjectMapper for JSON parsing
     * @return account_id as a String, or null if not found or error occurs
     */
    private String retrieveAccountIdFromUserInfo(String accessToken, ObjectMapper mapper) {
        try {
            URL url = new URL(userInfoEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // Set the Authorization header to pass the access token
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            // Check for a successful response
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.err.println("Failed to retrieve user info. HTTP error code: " + connection.getResponseCode());
                return null;
            }

            // Read the response
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining());
                JsonNode userInfo = mapper.readTree(response);
                // The user info response typically contains an "accounts" array
                JsonNode accounts = userInfo.get("accounts");
                if (accounts != null && accounts.isArray() && accounts.size() > 0) {
                    // Use the first account; you may choose based on additional criteria if needed.
                    JsonNode firstAccount = accounts.get(0);
                    JsonNode accountIdNode = firstAccount.get("account_id");
                    if (accountIdNode != null) {
                        return accountIdNode.asText();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error while retrieving user info: " + e.getMessage());
        }
        return null;
    }
}
