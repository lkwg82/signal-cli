package de.lgohlke.signal.commands;

import de.lgohlke.signal.ConfigProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

import java.io.IOException;
import java.net.ServerSocket;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

class ExpensesViewSummaryTest {

    private MockServerClient mockServer;
    private int localPort;

    @BeforeEach
    void startMockServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        localPort = serverSocket.getLocalPort();
        serverSocket.close();
        mockServer = startClientAndServer(localPort);
    }

    @AfterEach
    void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void testCommand() throws IOException {
        ConfigProvider testConfigProvider = new ConfigProvider() {
            @Override
            public String getUrlFromConfig(@NonNull String key) {
                return "http://localhost:" + localPort + "/test";
            }
        };

        mockServer.when(HttpRequest.request())
                  .respond(HttpResponse.response()
                                       .withStatusCode(200));

        new ExpensesViewSummary(testConfigProvider).execute();

        mockServer.verify(HttpRequest.request(), VerificationTimes.once());
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
