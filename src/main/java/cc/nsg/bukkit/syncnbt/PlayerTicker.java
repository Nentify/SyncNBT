package cc.nsg.bukkit.syncnbt;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handlers players for ProtocolLib (mode 2)
 */

public class PlayerTicker {

	private String username;
	private UUID uuid;
	private int ticker_thread_id = -1;
	private SyncNBT plugin;

	public PlayerTicker(SyncNBT plugin, String username, UUID uuid) {
		this.plugin = plugin;

		this.username = username;
		this.uuid = uuid;
	}

	// We found a new player to track
	public void startPlayerTicker() {
		plugin.getLogger().info("A new player called " + username + " found, register player tracking.");

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
					String json = new JSONSerializer().toJSON(username, uuid);
					plugin.db.saveJSONData(uuid, username, json);
				}
			}
		}, 1200L, 1200L);
	}

	// The player is gone
	public void stopPlayerTicker(Boolean save) {

		if (save) {
			String json = new JSONSerializer().toJSON(username, uuid);
			plugin.db.saveJSONData(uuid, username, json);
		}

		plugin.getLogger().info("Player " + uuid.toString() + " not found, unregister player tracking.");
		Bukkit.getScheduler().cancelTask(ticker_thread_id);
	}

	public void stopPlayerTicker() {
		stopPlayerTicker(false);
	}

	public String getName() {
		return username;
	}

	private UUID getUUID() {
		return uuid;
	}
}
