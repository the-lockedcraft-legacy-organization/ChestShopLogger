package de.cubelegends.chestshoplogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import de.cubelegends.chestshoplogger.db.DBHandler;
import de.cubelegends.chestshoplogger.listeners.ChestShopListener;
import de.cubelegends.chestshoplogger.listeners.JoinListener;

import java.util.logging.Logger;
import org.bukkit.Bukkit;

import org.mcstats.Metrics;

public class ChestShopLogger extends JavaPlugin {
	
	public static final String PREFIX = ChatColor.DARK_GREEN + "[ChestShopLogger] " + ChatColor.GRAY;
	private Logger log = Bukkit.getLogger();
	private DBHandler db;
	
	public void onEnable() {
            
		
		// Load config
		this.getConfig().addDefault("database.host", "localhost");
		this.getConfig().addDefault("database.port", 3306);
		this.getConfig().addDefault("database.user", "root");
		this.getConfig().addDefault("database.password", "");
		this.getConfig().addDefault("database.database", "cslogger");
                this.getConfig().addDefault("database.ssl", "false");
		this.getConfig().addDefault("database.tableVersion", 2);
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
						
		// Load database
		db = new DBHandler(
				this.getConfig().getString("database.host"),
				this.getConfig().getInt("database.port"),
				this.getConfig().getString("database.user"),
				this.getConfig().getString("database.password"),
				this.getConfig().getString("database.database"),
                                this.getConfig().getBoolean("database.ssl")
				);
		Connection con = db.open();
		if(con == null) {
                        log.info("[ChestShopLogger]=======================================================");
                        log.severe("[ChestShopLogger] Unable to connect to your database; check the config!");
                        log.info("[ChestShopLogger]=======================================================");
                        log.info("[ChestShopLogger] Note that Flatfile / SQLite storage is not currently supported.");
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		}
		setupTables(con);
		updateTables(con);
                
		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// Register events
		getServer().getPluginManager().registerEvents(new ChestShopListener(this), this);
		getServer().getPluginManager().registerEvents(new JoinListener(this), this);
		
		// Register command executor
		getCommand("shop").setExecutor(new CmdHandler(this));
		
		// Register metrics
                int pluginId = 10453;
                Metrics metrics = new Metrics(this, pluginId);
	}
	
	public void onDisable() {
		
	}
	
	public DBHandler getDBHandler() {
		return db;
	}
	
	private void setupTables(Connection con) {
		try {
			PreparedStatement st = con.prepareStatement(
					"CREATE TABLE IF NOT EXISTS chestshop_shop ("
					+ "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
					+ "world VARCHAR(50),"
					+ "x INT,"
					+ "y INT,"
					+ "z INT,"
					+ "tpx DOUBLE,"
					+ "tpy DOUBLE,"
					+ "tpz DOUBLE,"
					+ "tpyaw DOUBLE,"
					+ "tppitch DOUBLE,"
					+ "owneruuid VARCHAR(50),"
					+ "maxamount INT,"
					+ "buyprice DOUBLE,"
					+ "sellprice DOUBLE,"
					+ "itemname VARCHAR(50),"
					+ "created BIGINT"
					+ ");"
					);
			st.execute();
			st.close();
			st = con.prepareStatement(
					"CREATE TABLE IF NOT EXISTS chestshop_transaction ("
					+ "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
					+ "shopid INT,"
					+ "clientuuid VARCHAR(50),"
					+ "type VARCHAR(10),"
					+ "amount INT,"
					+ "price DOUBLE,"
					+ "date BIGINT"
					+ ");"
					);
			st.execute();
			st.close();
			st = con.prepareStatement(
					"CREATE TABLE IF NOT EXISTS chestshop_player ("
					+ "uuid VARCHAR(50) PRIMARY KEY,"
					+ "name VARCHAR(50)"
					+ ");"
					);
			st.execute();
			st.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void updateTables(Connection con) {
		try {
			
			switch(this.getConfig().getInt("database.tableVersion")) {
			
			case 1:
				PreparedStatement st = con.prepareStatement(
						"INSERT INTO chestshop_player (uuid, name) SELECT owneruuid, owner FROM chestshop_shop GROUP BY owneruuid"
						);
				st.execute();
				st.close();
				st = con.prepareStatement(
						"INSERT INTO chestshop_player (uuid, name) SELECT clientuuid, client FROM chestshop_transaction GROUP BY clientuuid ON DUPLICATE KEY UPDATE uuid = uuid;"
						);
				st.execute();
				st.close();
				st = con.prepareStatement(
						"ALTER TABLE chestshop_shop DROP owner"
						);
				st.execute();
				st.close();
				st = con.prepareStatement(
						"ALTER TABLE chestshop_transaction DROP client"
						);
				st.execute();
				st.close();
			
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if(this.getConfig().getInt("database.tableVersion") != 2) {
			this.getConfig().set("database.tableVersion", 2);
			this.saveConfig();
		}
	}

}
