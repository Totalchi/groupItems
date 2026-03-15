package be.vdab.osrsplugin.runelite;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class GroupSyncClientTest
{
	@Test
	public void fetchOverviewShouldRejectInvalidBaseUrl() throws Exception
	{
		GroupSyncClient client = new GroupSyncClient(new Gson());

		try
		{
			client.fetchOverview("http://[invalid", "grp-test");
			fail("Expected invalid base URL to fail");
		}
		catch (IOException exception)
		{
			assertTrue(exception.getMessage().contains("Sync server URL is invalid"));
		}
	}

	@Test
	public void fetchOverviewShouldRejectInvalidJsonResponse() throws Exception
	{
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/api/group-inventory", exchange -> respond(exchange, 200, "not-json"));
		server.start();

		try
		{
			GroupSyncClient client = new GroupSyncClient(new Gson());
			client.fetchOverview("http://127.0.0.1:" + server.getAddress().getPort(), "grp-test");
			fail("Expected invalid JSON response to fail");
		}
		catch (IOException exception)
		{
			assertTrue(exception.getMessage().contains("Sync server returned invalid JSON"));
		}
		finally
		{
			server.stop(0);
		}
	}

	private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException
	{
		byte[] payload = body.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, payload.length);
		try (OutputStream outputStream = exchange.getResponseBody())
		{
			outputStream.write(payload);
		}
	}
}
