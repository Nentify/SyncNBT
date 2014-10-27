package cc.nsg.bukkit.syncnbt;

import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import com.comphenix.protocol.*;

/**
 * This is the main class that extends JavaPlugin.
 * @author Stefan Berggren
 *
 */

public class SyncNBT extends JavaPlugin {

  private Logger log = null;
  
  protected Database db = null;
  
  /**
   * onEnable is triggered when the plugin is successfully loaded.
   */
  @Override
  public void onEnable() {
    super.onEnable();
    
    log = this.getLogger();
    log.info("Loading " + getName() + " version " + getDescription().getVersion());
    
    saveDefaultConfig();

    // Open a connection to the database and setup tables
    db = new Database(this);
    
    // Load plugins that I depend on
    ProtocolLibrary.getProtocolManager(); // ProtocolLib
  }

  /**
   * Plugin cleanup when the plugin is unloaded, server is most likely shutting down.
   */
  @Override
  public void onDisable() {
    super.onDisable();
    
    log.info("Plugin " + getName() + " is now disabled.");
  }
  
}
