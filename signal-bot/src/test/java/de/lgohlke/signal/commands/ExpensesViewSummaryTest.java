package de.lgohlke.signal.commands;

import de.lgohlke.signal.ConfigProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.verify.VerificationTimes.once;

class ExpensesViewSummaryTest {

    private ClientAndServer mockServer;
    private int localPort;

    @BeforeEach
    void startMockServer() throws IOException {
        mockServer = startClientAndServer();
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop(true);
    }

    @Test
    void testCommand() throws IOException {
        String url = "http://localhost:" + mockServer.getLocalPort() + "/test";

        ConfigProvider testConfigProvider = new ConfigProvider() {
            @Override
            public String getUrlFromConfig(@NonNull String key) {
                return url;
            }
        };

        mockServer.when(request()
                .withPath("/test"))
                  .respond(HttpResponse.response()
                                       .withStatusCode(200)
                                       .withBody("{\"pdf\":\"" + createBase64FromTestPdf() + "\"}")
                  );

        new ExpensesViewSummary(testConfigProvider).execute();

        mockServer.verify(request().withPath("/test"), once());
    }

    private String createBase64FromTestPdf() throws IOException {
        InputStream pdfStream = Thread.currentThread()
                                      .getContextClassLoader()
                                      .getResourceAsStream("google-tos.pdf");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copy(pdfStream, outputStream);
        return Base64.getEncoder()
                     .encodeToString(outputStream.toByteArray());
    }

    interface RequestReplyCommand {

        void execute();
    }

    @RequiredArgsConstructor
    static class ExpensesViewSummary implements RequestReplyCommand {

        private final ConfigProvider configProvider;

        public void execute() {
            String urlFromConfig = configProvider.getUrlFromConfig("ausgabengemeinsam.summary.url");
            Response response = doRequest(urlFromConfig);
            System.out.println(response);
        }

        @SneakyThrows
        private Response doRequest(String urlFromConfig) {
            OkHttpClient httpClient = createHttpClient();
            Request request = createRequest(urlFromConfig);
            return httpClient.newCall(request)
                             .execute();
        }

        private Request createRequest(String urlFromConfig) {
            return new Request.Builder().get()
                                        .url(urlFromConfig)
                                        .build();
        }

        private OkHttpClient createHttpClient() {
            return new OkHttpClient.Builder().followRedirects(true)
                                             .followSslRedirects(true)
                                             .build();
        }
    }
}
