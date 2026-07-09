package ru.nikit.megastructure.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class VulkanClientConfig {
	private static final String CONFIG_FILE = "imperfect_salvation_client.properties";
	private static final String VULKAN_ENABLED_KEY = "vulkan_enabled";
	private static boolean loaded;
	private static boolean vulkanEnabled = true;

	private VulkanClientConfig() {
	}

	public static synchronized boolean isVulkanEnabled() {
		loadIfNeeded();
		String override = System.getProperty("megastructure.vulkan", "").trim().toLowerCase();
		if (override.equals("off") || override.equals("false") || override.equals("0") || override.equals("disabled")) {
			return false;
		}
		return vulkanEnabled;
	}

	public static synchronized void setVulkanEnabled(boolean enabled) {
		loadIfNeeded();
		vulkanEnabled = enabled;
		System.setProperty("megastructure.vulkan", enabled ? "auto" : "off");
		save();
	}

	static synchronized void loadOnClientStart() {
		loadIfNeeded();
		if (!vulkanEnabled) {
			System.setProperty("megastructure.vulkan", "off");
		}
	}

	private static void loadIfNeeded() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path configPath = configPath();
		if (!Files.exists(configPath)) {
			save();
			return;
		}
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			properties.load(reader);
			vulkanEnabled = Boolean.parseBoolean(properties.getProperty(VULKAN_ENABLED_KEY, "true").trim());
		} catch (IOException error) {
			System.err.println("Failed to read Imperfect_salvation client config: " + error.getMessage());
		}
	}

	private static void save() {
		Properties properties = new Properties();
		properties.setProperty(VULKAN_ENABLED_KEY, Boolean.toString(vulkanEnabled));
		Path configPath = configPath();
		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				properties.store(writer, "Imperfect_salvation client settings");
			}
		} catch (IOException error) {
			System.err.println("Failed to write Imperfect_salvation client config: " + error.getMessage());
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
	}
}
