package be.vdab.osrsplugin.runelite;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

final class GroupSyncClient
{
	private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

	private final OkHttpClient okHttpClient;
	private final Gson gson;

	@Inject
	GroupSyncClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	SyncModels.GroupOverviewResponse uploadBank(String baseUrl, String groupCode, String memberName, BankSnapshot snapshot)
		throws IOException
	{
		Request request = new Request.Builder()
			.url(buildUrl(baseUrl, "/api/group-inventory/members/" + encodePath(memberName)))
			.header("X-Group-Code", groupCode)
			.header("Accept", "application/json")
			.put(RequestBody.create(JSON, gson.toJson(snapshot.toUploadRequest())))
			.build();

		return send(request);
	}

	SyncModels.GroupOverviewResponse fetchOverview(String baseUrl, String groupCode)
		throws IOException
	{
		Request request = new Request.Builder()
			.url(buildUrl(baseUrl, "/api/group-inventory"))
			.header("X-Group-Code", groupCode)
			.header("Accept", "application/json")
			.get()
			.build();

		return send(request);
	}

	private SyncModels.GroupOverviewResponse send(Request request) throws IOException
	{
		try (Response response = okHttpClient.newCall(request).execute())
		{
			String body = response.body() != null ? response.body().string() : null;
			if (!response.isSuccessful())
			{
				throw new IOException("Sync server returned " + response.code() + formatResponseBody(body));
			}

			try
			{
				SyncModels.GroupOverviewResponse overview = gson.fromJson(body, SyncModels.GroupOverviewResponse.class);
				if (overview == null)
				{
					throw new IOException("Sync server returned an empty response");
				}
				return overview;
			}
			catch (JsonParseException | IllegalStateException exception)
			{
				throw new IOException("Sync server returned invalid JSON" + formatResponseBody(body), exception);
			}
		}
	}

	private HttpUrl buildUrl(String baseUrl, String path) throws IOException
	{
		String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim();
		while (normalizedBaseUrl.endsWith("/"))
		{
			normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
		}

		HttpUrl url = HttpUrl.parse(normalizedBaseUrl + path);
		if (url == null)
		{
			throw new IOException("Sync server URL is invalid: " + normalizedBaseUrl);
		}
		return url;
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
