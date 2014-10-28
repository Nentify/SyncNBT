package cc.nsg.bukkit.syncnbt;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handlers players for ProtocolLib (mode 2)
 */

public class PlayerTicker {

	private String name;
	private UUID uuid;
	private int ticker_thread_id = -1;
	private SyncNBT plugin;

	public PlayerTicker(SyncNBT plugin, String name, UUID uuid) {
		this.plugin = plugin;

		this.name = name;
		this.uuid = uuid;
	}

	// We found a new player to track
	public void startPlayerTicker() {
		plugin.getLogger().info("A new player called " + name + " found, register player tracking.");

		String json = plugin.db.getJSONData(uuid);
		if (json != null) {
			plugin.getLogger().info("Found data in database for player " + uuid.toString() + ", restoring data.");
			new JSONSerializer().restorePlayer(json);
		}

		ticker_thread_id = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

			@Override
			public void run() {
				Player p = Bukkit.getServer().getPlayer(uuid);
				if (p == null) {
					stopPlayerTicker();
				} else {
					String json = new JSONSerializer().toJSON(name, uuid);
					plugin.db.saveJSONData(uuid, json);
				}
			}
		}, 1200L, 1200L);
	}

	// The player is gone
	public void stopPlayerTicker(Boolean save) {

		if (save) {
			String json = new JSONSerializer().toJSON(name, uuid);
			plugin.db.saveJSONData(uuid, json);
		}

		plugin.getLogger().info("Player " + uuid.toString() + " not found, unregister player tracking.");
		Bukkit.getScheduler().cancelTask(ticker_thread_id);
	}

	public void stopPlayerTicker() {
		stopPlayerTicker(false);
	}

	public String getName() {
		return name;
	}

	private UUID getUUID() {
		return uuid;
	}
}
