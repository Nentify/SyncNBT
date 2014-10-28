package cc.nsg.bukkit.syncnbt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class Database {

	String hostname = null;
	int port = 3306;
	String database = null;
	String user = null;
	String password = null;
	Connection connection = null;
	Logger log = null;


	public Database(SyncNBT plugin) {

		if (plugin == null || !plugin.isEnabled()) {
			System.out.println("Error: plugin not passed to constructor, or the plugin is disabled.");
			return;
		}

		log = plugin.getLogger();

		// TODO: Check these values for sane data
		hostname = plugin.getConfig().getString("database.mysql.hostname");
		port = plugin.getConfig().getInt("database.mysql.port");
		database = plugin.getConfig().getString("database.mysql.database");
		user = plugin.getConfig().getString("database.mysql.username");
		password = plugin.getConfig().getString("database.mysql.password");

		if (!openConnection()) {
			return;
		}

		if (!createTables()) {
			log.severe("Error: failed to create/check tables");
			return;
		}
	}

	public Connection getConnection() {
		openConnection();
		return connection;

	}

	public void saveJSONData(String player_uuid, String JSONData) {
		openConnection();

		// Save stuff
		try {
			String sql = "INSERT INTO syncnbt_json (player_uuid, json_data) VALUES(?,?) ON DUPLICATE KEY UPDATE json_data = ?";
			PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, player_uuid);
			statement.setString(2, JSONData);
			statement.setString(3, JSONData);
			statement.execute();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		// Save a copy to _versions, this is used for restores
		try {
			String sql = "INSERT INTO syncnbt_json_versions (player_uuid, json_data) VALUES(?,?)";
			PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, player_uuid);
			statement.setString(2, JSONData);
			statement.execute();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

	}

	public String getJSONData(String player_uuid) {
		openConnection();

		String sql = "SELECT * FROM syncnbt_json WHERE player_uuid = ?";
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, player_uuid);
			ResultSet res = statement.executeQuery();
			if (res.next()) {
				return res.getString("json_data");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	private void playerState(String player_uuid, int state) {
		openConnection();

		String sql = "INSERT INTO syncnbt_locks (player_uuid, state) values(?, ?) on duplicate key update state = ?";
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, player_uuid);
			statement.setInt(2, state);
			statement.setInt(3, state);
			statement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean isPlayerLocked(String player_uuid) {
		openConnection();

		String sql = "SELECT * FROM syncnbt_locks WHERE player_uuid = ?";
		try {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setString(1, player_uuid);
			ResultSet res = statement.executeQuery();
			if (res.next()) {
				return res.getInt("state") == 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public void lockPlayer(String player_uuid) {
		playerState(player_uuid, 1);
	}

	public void unlockPlayer(String player_uuid) {
		playerState(player_uuid, 0);
	}

	public boolean openConnection() {

		try {

			if (connection != null && connection.isValid(500)) {
				return true;
			}

			log.info("No valid connection found, reconnect to MySQL server.");

			Class.forName("com.mysql.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database + "?autoReconnect=true", user, password);
			return true;
		} catch (SQLException e) {
			log.info(e.getMessage());
		} catch (ClassNotFoundException e) {
			log.info("Error, unable to find JDBC driver");
		}

		log.severe("Error: failed to open a connection to the MySQL server");
		return false;
	}

	private boolean createTables() {
		try {

      /* used by ProtocolLib mode 2 */

			String sql = "CREATE TABLE IF NOT EXISTS syncnbt_json (" +
					"player_uuid VARCHAR(255) PRIMARY KEY, json_data BLOB" +
					");";
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.executeUpdate();

			sql = "CREATE TABLE IF NOT EXISTS syncnbt_json_versions (" +
					"id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, player_uuid VARCHAR(255), json_data BLOB, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
					");";
			statement = connection.prepareStatement(sql);
			statement.executeUpdate();

      /* Locks */

			sql = "CREATE TABLE IF NOT EXISTS syncnbt_locks (" +
					"player_uuid VARCHAR(255) PRIMARY KEY, state SMALLINT" +
					");";
			statement = connection.prepareStatement(sql);
			statement.executeUpdate();

			return true;
		} catch (SQLException e) {
			log.info(e.getMessage());
		}

		return false;
	}

}
