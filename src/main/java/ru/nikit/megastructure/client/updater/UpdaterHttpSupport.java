package ru.nikit.megastructure.client.updater;

final class UpdaterHttpSupport {
	private UpdaterHttpSupport() {
	}

	static boolean shouldRetryHttp(int statusCode) {
		return statusCode == 408 || statusCode == 429 || statusCode >= 500;
	}
}
