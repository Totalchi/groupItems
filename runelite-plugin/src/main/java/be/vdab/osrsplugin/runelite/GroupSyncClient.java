package be.vdab.osrsplugin.runelite;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.inject.Inject;

final class GroupSyncClient
{
	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(TIMEOUT)
		.build();
	private final Gson gson;

	@Inject
	GroupSyncClient(Gson gson)
	{
		this.gson = gson;
	}

	SyncModels.GroupOverviewResponse uploadBank(String baseUrl, String groupCode, String memberName, BankSnapshot snapshot)
		throws IOException, InterruptedException
	{
		HttpRequest request = requestBuilder(baseUrl, "/api/group-inventory/members/" + encodePath(memberName))
			.header("X-Group-Code", groupCode)
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(snapshot.toUploadRequest())))
			.build();

		return send(request);
	}

	SyncModels.GroupOverviewResponse fetchOverview(String baseUrl, String groupCode)
		throws IOException, InterruptedException
	{
		HttpRequest request = requestBuilder(baseUrl, "/api/group-inventory")
			.header("X-Group-Code", groupCode)
			.header("Accept", "application/json")
			.GET()
			.build();

		return send(request);
	}

	private SyncModels.GroupOverviewResponse send(HttpRequest request) throws IOException, InterruptedException
	{
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() < 200 || response.statusCode() >= 300)
		{
			throw new IOException("Sync server returned " + response.statusCode() + formatResponseBody(response.body()));
		}

		try
		{
			SyncModels.GroupOverviewResponse overview = gson.fromJson(response.body(), SyncModels.GroupOverviewResponse.class);
			if (overview == null)
			{
				throw new IOException("Sync server returned an empty response");
			}
			return overview;
		}
		catch (JsonParseException | IllegalStateException exception)
		{
			throw new IOException("Sync server returned invalid JSON" + formatResponseBody(response.body()), exception);
		}
	}

	private HttpRequest.Builder requestBuilder(String baseUrl, String path) throws IOException
	{
		return HttpRequest.newBuilder()
			.uri(buildUri(baseUrl, path))
			.timeout(TIMEOUT);
	}

	private URI buildUri(String baseUrl, String path) throws IOException
	{
		String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
		while (normalizedBaseUrl.endsWith("/"))
		{
			normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
		}

		try
		{
			return URI.create(normalizedBaseUrl + path);
		}
		catch (IllegalArgumentException exception)
		{
			throw new IOException("Sync server URL is invalid: " + normalizedBaseUrl, exception);
		}
	}

	private String encodePath(String value)
	{
		return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
	}

	private String formatResponseBody(String body)
	{
		if (body == null || body.isBlank())
		{
			return "";
		}
		String trimmed = body.replaceAll("\\s+", " ").trim();
		if (trimmed.length() > 180)
		{
			trimmed = trimmed.substring(0, 177) + "...";
		}
		return ": " + trimmed;
	}
}
