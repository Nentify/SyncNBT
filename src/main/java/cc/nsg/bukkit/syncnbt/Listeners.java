package cc.nsg.bukkit.syncnbt;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * This class listens for events from Bukkit.
 *
 * @author Stefan Berggren
 */

public class Listeners implements Listener {

	SyncNBT plugin = null;

	public Listeners(SyncNBT plugin) {
		this.plugin = plugin;
	}

	/**
	 * A player leaves the server, note that this event is only triggered with
	 * a clean normal quit.
	 */

	@EventHandler
	public void playerLogout(PlayerQuitEvent event) {
		plugin.db.openConnection();

		Player player = event.getPlayer();
		plugin.db.lockPlayer(player.getUniqueId(), player.getName());

		plugin.getLogger().info("Player " + player.getUniqueId().toString() + " logout");
		new PlayerTicker(plugin, player.getName(), player.getUniqueId()).stopPlayerTicker(true);

		plugin.db.unlockPlayer(player.getUniqueId(), player.getName());
	}

	/**
	 * A player logs in to the server, race conditions!
	 */

	@EventHandler
	public void playerLogin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();

    /*
     * Spawn an async thread and wait for the lock to clear
     */
		new BukkitRunnable() {

			@Override
			public void run() {
				while (plugin.db.isPlayerLocked(player.getUniqueId())) {
					player.sendMessage("Items still locked by another server...");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				// Call a sync thread to modify the world
				Bukkit.getScheduler().runTask(plugin, new Runnable() {

					@Override
					public void run() {
						plugin.getLogger().info("Player " + player.getUniqueId().toString() + " login");
						new PlayerTicker(plugin, player.getName(), player.getUniqueId()).startPlayerTicker();
					}
				});

			}

		}.runTaskLaterAsynchronously(plugin, 20L);

	}

}
