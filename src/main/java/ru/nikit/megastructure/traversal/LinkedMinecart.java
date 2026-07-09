package ru.nikit.megastructure.traversal;

import java.util.List;
import java.util.UUID;

public interface LinkedMinecart {
	List<UUID> megastructure$getLinks();

	boolean megastructure$addLink(UUID uuid);

	boolean megastructure$removeLink(UUID uuid);

	default boolean megastructure$hasFreeLink() {
		return megastructure$getLinks().size() < 2;
	}
}
