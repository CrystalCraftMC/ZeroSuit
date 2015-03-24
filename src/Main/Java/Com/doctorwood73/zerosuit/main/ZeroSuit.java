package com.doctorwood73.zerosuit.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.swing.Timer;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**Main class:
 * This program will automatically enable fly-mode when someone enters an area
 */
public class ZeroSuit extends JavaPlugin implements Listener {
	
	/**This holds all regions of zero g's*/
	private ArrayList<ZeroGArea> zeroArea = new ArrayList<ZeroGArea>();
	
	/**Holds all players who have flying perms*/
	private ArrayList<String> flyPerms = new ArrayList<String>();
	
	/**Holds players who got a "entered zero-g" message in the last half a second*/
	private ArrayList<Player> msgCap = new ArrayList<Player>();
	
	/**Holds players with zerosuit on*/
	private ArrayList<Player> zs = new ArrayList<Player>();
	
	/**Update checks on people currently wearing zerosuits*/
	private Timer tim;
	
	public void onEnable() {
		this.initializeZeroGAreaFile();
		this.initializePermsFile();
		this.getServer().getPluginManager().registerEvents(this, this);
		tim = new Timer(500, new Update());
		tim.start();
	}
	public void onDisable() {
		msgCap.clear();
		tim.stop();
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String args[]) {
		if(sender instanceof Player) {
			Player p = (Player)sender;
			if(p.hasPermission("ZeroSuit.zerogflyperms") && label.equalsIgnoreCase("zerogflyperms")) {
				if(args.length == 0) {
					if(flyPerms.size() == 0) {
						p.sendMessage(ChatColor.BLUE + "Noone has fly permissions at this time.");
						return true;
					}
					StringBuilder sb = new StringBuilder();
					for(int i = 0; i < flyPerms.size(); i++) {
						sb.append(flyPerms.get(i));
						if(i != flyPerms.size()-1)
							sb.append(", ");
						else
							sb.append(".");
					}
					p.sendMessage(ChatColor.LIGHT_PURPLE + "People with fly perms:");
					p.sendMessage(ChatColor.AQUA + sb.toString());
					p.sendMessage(ChatColor.GOLD + "Remember; /zerogflyperms <add | remove> <playername> " +
							"to add/remove from the list.");
					return true;
				}
				else if(args.length == 2) {
					if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
						boolean isAdding = args[0].equalsIgnoreCase("add") ? true : false;
						if(isAdding) {
							boolean isOnline = false;
							for(Player pp : new ArrayList<Player>(this.getServer().getOnlinePlayers())) {
								if(pp.getName().equals(args[1])) {
									flyPerms.add(args[1]);
									p.sendMessage(ChatColor.LIGHT_PURPLE + args[1] + " successfully added.");
									pp.setAllowFlight(true);
									isOnline = true;
								}
							}
							if(!isOnline) {
								p.sendMessage(ChatColor.RED + "Error; " + ChatColor.GOLD + args[1] +
										ChatColor.RED + " is not online.");
							}
							return true;
						}
						else {
							boolean isOnline = false;
							boolean existsInList = false;
							for(int i = 0; i < flyPerms.size(); i++) {
								if(args[1].equalsIgnoreCase(flyPerms.get(i))) {
									existsInList = true;
								}
							}
							if(!existsInList) {
								p.sendMessage(ChatColor.RED + "Error; " + ChatColor.GOLD + args[1] +
										ChatColor.RED + " was not found in the file. Use /zerogflyperms to " +
										"list all people with fly permissions.");
								return true;
							}
							for(Player pp : new ArrayList<Player>(this.getServer().getOnlinePlayers())) {
								if(pp.getName().equals(args[1])) {
									flyPerms.remove(args[1]);
									
									if(pp.isFlying())
										pp.setFlying(false);
									pp.setAllowFlight(false);
									isOnline = true;
									return true;
								}
							}
							if(!isOnline) {
								p.sendMessage(ChatColor.RED + "Error; " + ChatColor.GOLD + args[1] +
										ChatColor.RED + " is not online.");
								return true;
							}
						}
						this.updateFlyPermsFile();
						String confirmationMsg = isAdding ? ChatColor.BLUE + "Player " + ChatColor.GOLD +
								args[1] + ChatColor.BLUE + " Successfully Added To Fly-Perms List" :
									ChatColor.BLUE + "Player " + ChatColor.GOLD +
									args[1] + ChatColor.BLUE + " Successfully Removed From Fly-Perms List";
						p.sendMessage(ChatColor.GREEN + "Note that the name given is case-sensitive.");
						p.sendMessage(confirmationMsg);
						p.sendMessage(ChatColor.GOLD + "Remember; /zerogflyperms with no args " +
								"to view the whole list.");
						return true;
					}
					p.sendMessage(ChatColor.RED + "Error; the first argument must read \'add\' or \'remove\'");
					return false;
				}
			}
			else if(!p.hasPermission("ZeroSuit.zerogflyperms") && label.equalsIgnoreCase("zerogflyperms")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
				return true;
			}
			else if(p.hasPermission("ZeroSuit.zerog") && label.equalsIgnoreCase("zerog")) {
				if(args.length == 8) {
					if(args[0].equalsIgnoreCase("add")) {
						boolean validArguments = true;
						if(!this.isInt(args[1]))
							validArguments = false;
						if(!this.isInt(args[2]))
							validArguments = false;
						if(!this.isInt(args[3]))
							validArguments = false;
						if(!this.isInt(args[4]))
							validArguments = false;
						if(!this.isInt(args[5]))
							validArguments = false;
						if(!this.isInt(args[6]))
							validArguments = false;
						if(validArguments) {
							String world = "";
							if(p.getWorld().getEnvironment() == Environment.NORMAL)
								world = "overworld";
							else if(p.getWorld().getEnvironment() == Environment.NETHER)
								world = "nether";
							else if(p.getWorld().getEnvironment() == Environment.THE_END)
								world = "end";
							for(int i = 0; i < zeroArea.size(); i++) {
								if(zeroArea.get(i).getID().equalsIgnoreCase(args[7])) {
									p.sendMessage(ChatColor.RED + "Error; a zero-suit area with ID: " +
											ChatColor.GOLD + args[7] + ChatColor.RED + " already " +
											"exists.  Do /zerogflyperms to view all current zero-suit areas.");
									return true;
								}
							}
							zeroArea.add(new ZeroGArea(Integer.parseInt(args[1]), 
									Integer.parseInt(args[2]),
									Integer.parseInt(args[3]), Integer.parseInt(args[4]),
									Integer.parseInt(args[5]), Integer.parseInt(args[6]), 
										world, args[7]));
							this.updateZeroGAreaFile();
							p.sendMessage(ChatColor.GOLD + "Area successfully added.");
							return true;
						}
						else {
							p.sendMessage(ChatColor.RED + "Error; between arguments 2-7 (inclusive) some " +
									"are not valid integer values.");
							return false;
						}
					}
					else {
						p.sendMessage(ChatColor.RED + "Error; the first argument must read \'add\' or \'remove\'");
						return false;
					}
				}
				else if(args.length == 2) {
					if(args[0].equalsIgnoreCase("remove")) {
						for(int i = 0; i < zeroArea.size(); i++) {
							if(zeroArea.get(i).getID().equalsIgnoreCase(args[1])) {
								zeroArea.remove(i);
								this.updateZeroGAreaFile();
								p.sendMessage(ChatColor.BLUE + "Area successfully removed.");
								return true;
							}
						}
						p.sendMessage(ChatColor.RED + args[1] + ChatColor.GOLD + " is not " +
								"an existing zero-gravity region ID.  Type /zerog to view all current " +
								"zero gravity regions.");
						return true;
					}
					else {
						p.sendMessage(ChatColor.RED + "Error; your first argument was not \'remove\'");
						return false;
					}
				}
				else if(args.length == 0) {
					if(zeroArea.size() == 0) {
						p.sendMessage(ChatColor.BLUE + "There are no zero-gravity regions currently made");
						return true;
					}
					for(int i = 0; i < zeroArea.size(); i++) {
						p.sendMessage(zeroArea.get(i).toString());
					}
					return true;
				}
				else {
					return false;
				}
			}
			else if(!p.hasPermission("ZeroSuit.zerog") && label.equalsIgnoreCase("zerog")) {
				p.sendMessage(ChatColor.RED + "You do not have permission to perform this command.");
				return true;
			}
			else if(p.hasPermission("ZeroSuit.zeroghelp") && label.equalsIgnoreCase("zeroghelp")) {
				StringBuilder sb = new StringBuilder();
				sb.append(ChatColor.LIGHT_PURPLE + "/zerog\n");
				sb.append(ChatColor.AQUA + "This 0 argument command will list all areas that " +
						"are zero-gravity regions\n");
				sb.append(ChatColor.LIGHT_PURPLE + "/zerog <remove> <nameID>\n" +
						ChatColor.AQUA + "This 2 argument command will remove a specified zero-gravity region\n");
				sb.append(ChatColor.LIGHT_PURPLE + "/zerog <add> <x1> <y1> <z1> <x2> <y2> <z2> <nameID>\n" +
						ChatColor.AQUA + "This 8 argument command will create a zero-gravity region in the " +
							"world you're in (overworld | nether | end) at the given coordinates\n");
				sb.append(ChatColor.BLUE + "/zerogflyperms\n" + ChatColor.AQUA +
						"This 0 argument command will list everybody who has fly-permissions.  People in creative all have it by " +
						"default. " + ChatColor.RED +  "Without fly permission, moving in a non-zero gravity area " +
						"will disable your flying.\n");
				sb.append(ChatColor.BLUE + "/zerogflyperms <add | remove> <playerName>\n" + ChatColor.AQUA +
						" This 2 argument case-sensitive command will add or remove people from the fly-permissions list\n");
				sb.append(ChatColor.BLUE + "/zerogfast\n" + ChatColor.AQUA +
						"Sets your flyspeed to fast\n");
				sb.append(ChatColor.BLUE + "/zerogslow\n" + ChatColor.AQUA +
						"Sets your flyspeed to slow\n");
				sb.append(ChatColor.GREEN + "/zeroghelp\n" + ChatColor.AQUA +
						"Lists All ZeroSuit Commands");
				p.sendMessage(sb.toString());
				return true;
			}
			else if(!p.hasPermission("ZeroSuit.zeroghelp") && label.equalsIgnoreCase("zeroghelp")) {
				p.sendMessage(ChatColor.RED + "Error; you do not have permission for this command.");
				return true;
			}
			else if(label.equalsIgnoreCase("zerogfast")) {
				p.setFlySpeed((float).2);
				p.sendMessage(ChatColor.DARK_PURPLE + "Fly Speed Set To " + ChatColor.RED + "fast");
				return true;
			}
			else if(label.equalsIgnoreCase("zerogslow")) {
				p.setFlySpeed((float).1);
				p.sendMessage(ChatColor.DARK_PURPLE + "Fly Speed Set To " + ChatColor.RED + "slow");
				return true;
			}
			return false;
		}
		else
			return false;
	}
	
	/**Will enable zerosuit if clicked*/
	@EventHandler
	public void zeroSuit(PlayerInteractEvent e) {
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if(e.getClickedBlock().getType() == Material.STONE_BUTTON || 
					e.getClickedBlock().getType() == Material.WOOD_BUTTON) {
				for(int i = 0; i < zeroArea.size(); i++) {
					if(isInZeroG(e.getClickedBlock().getLocation(), zeroArea.get(i))) {
						boolean isInList = false;
						for(int ii = 0; ii < zs.size(); ii++) {
							if(zs.get(ii).getName().equals(e.getPlayer().getName())) {
								isInList = true;
								zs.get(i).remove();
								if(!hasFlyPerms(e.getPlayer()))
									zs.get(ii).setAllowFlight(false);
							}
						}
						if(!isInList) {
							zs.add(e.getPlayer());
							e.getPlayer().setAllowFlight(true);
							e.getPlayer().setFlying(true);
						}
					}
			
				}
			}
		}
	}
	
	///**Will let the player fly if they're in a zero-g area,
	 //* and will not let them fly if they aren't
	 //*/
	/*@EventHandler
	public void zeroGCheck(PlayerMoveEvent e) {
		int zeroGIndex = -1;
		for(int i = 0; i < zeroArea.size(); i++) {
			boolean isInZeroG = this.isInZeroG(e.getPlayer().getLocation(), zeroArea.get(i));
			if(isInZeroG)
				zeroGIndex = i;
		}
		if(zeroGIndex != -1) {
			if(!e.getPlayer().isFlying()) {
				e.getPlayer().setAllowFlight(true);
				e.getPlayer().setFlying(true);
				boolean isInMsgCap = false;
				for(int i = 0; i < msgCap.size(); i++) {
					if(e.getPlayer().getName().equals(msgCap.get(i).getName()))
						isInMsgCap = true;
				}
				if(!isInMsgCap) {
					e.getPlayer().sendMessage(ChatColor.DARK_RED + "Now Entering " +
							ChatColor.AQUA + "Zero-Gravity");
					msgCap.add(e.getPlayer());
					final Player p = e.getPlayer();
					this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
						public void run() {
							msgCap.remove(p);
						}
					}, 10L);
				}
			}
		}
		else {
			boolean hasFlyPerms = this.hasFlyPerms(e.getPlayer());
			if(!hasFlyPerms) {
				if(e.getPlayer().isFlying()) {
					e.getPlayer().setAllowFlight(false);
					e.getPlayer().setFlying(false);
				}
			}
			else
				e.getPlayer().setAllowFlight(true);
		}
	}*/
	
	///**Will disable flying after a tp event*/
	@EventHandler
	public void noTpFly(PlayerTeleportEvent e) {
		final Player p = e.getPlayer();
		for(int i = 0; i < zeroArea.size(); i++) {
			if(!this.isInZeroG(e.getTo(), zeroArea.get(i))) {
				if(!this.hasFlyPerms(p)) {
						this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
				 	public void run() {
					        p.setAllowFlight(false);
							p.setFlying(false);
						}
					}, 2L);
				}
				return;
			}
		}
	}
	
	/**This method checks whether a player is in a certain zeroArea region
	 * @return boolean, true if they're in the zeroArea region
	 */
	public boolean isInZeroG(Location loc, ZeroGArea zga) {
		String world = "";
		if(loc.getWorld().getEnvironment() == Environment.NORMAL)
			world = "overworld";
		else if(loc.getWorld().getEnvironment() == Environment.NETHER)
			world = "nether";
		else if(loc.getWorld().getEnvironment() == Environment.THE_END)
			world = "end";
		
		
		if(!world.equalsIgnoreCase(zga.getWorld())) {
			return false;
		}
		boolean isInX, isInY, isInZ;
		if(zga.x1 < zga.x2) {
			isInX = loc.getX() < zga.x2 && loc.getX() > zga.x1 ? true : false;
		}
		else {
			isInX = loc.getX() > zga.x2 && loc.getX() < zga.x1 ? true : false;
		}
		if(zga.y1 < zga.y2) {
			isInY = loc.getY() < zga.y2 && loc.getY() > zga.y1 ? true : false;
		}
		else {
			isInY = loc.getY() > zga.y2 && loc.getY() < zga.y1 ? true : false;
		}
		if(zga.z1 < zga.z2) {
			isInZ = loc.getZ() < zga.z2 && loc.getZ() > zga.z1 ? true : false;
		}
		else {
			isInZ = loc.getZ() > zga.z2 && loc.getZ() < zga.z1 ? true : false;
		}
		boolean isInsideZeroG = isInZ && isInY && isInX;
		return isInsideZeroG;
	}
	
	/**This will initialize & read in data from the zerogarea .ser file*/
	public void initializeZeroGAreaFile() {
		File file = new File("ZeroSuitFiles\\ZeroGArea.ser");
		if(!file.exists()) {
			if(!new File("ZeroSuitFiles").exists())
				new File("ZeroSuitFiles").mkdir();
		}
		else {
			try{
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				zeroArea = (ArrayList)ois.readObject();
				ois.close();
				fis.close();
			}catch(IOException e) { e.printStackTrace(); 
			}catch(ClassNotFoundException e) { e.printStackTrace(); }
		}
	}
	
	/**Updates the zerogarea file*/
	public void updateZeroGAreaFile() {
		File file = new File("ZeroSuitFiles\\ZeroGArea.ser");
		if(!file.exists()) {
			if(!new File("ZeroSuitFiles").exists())
				new File("ZeroSuitFiles").mkdir();
		}
		else
			file.delete();
		try{
			FileOutputStream fos = new FileOutputStream(new File("ZeroSuitFiles\\ZeroGArea.ser"));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(zeroArea);
			oos.close();
			fos.close();
		}catch(IOException e) { e.printStackTrace(); }
	}
	
	/**Initializes & reads in the people who have permission*/
	public void initializePermsFile() {
		File file = new File("ZeroSuitFiles\\FlyPerms.ser");
		if(!file.exists()) {
			if(!new File("ZeroSuitFiles").exists())
				new File("ZeroSuitFiles").mkdir();
		}
		else {
			try{
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				flyPerms = (ArrayList)ois.readObject();
				ois.close();
				fis.close();
			}catch(IOException e) { e.printStackTrace(); 
			}catch(ClassNotFoundException e) { e.printStackTrace(); }
		}
	}
	
	/**Updates the permissions file*/
	public void updateFlyPermsFile() {
		File file = new File("ZeroSuitFiles\\FlyPerms.ser");
		if(!file.exists()) {
			if(!new File("ZeroSuitFiles").exists())
				new File("ZeroSuitFiles").mkdir();
		}
		else
			file.delete();
		try{
			FileOutputStream fos = new FileOutputStream(new File("ZeroSuitFiles\\FlyPerms.ser"));
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(flyPerms);
			oos.close();
			fos.close();
		}catch(IOException e) { e.printStackTrace(); }
	}
	
	/**Checks whether a player has permission to fly
	 * @return boolean, true if the player has permission to fly
	 */
	public boolean hasFlyPerms(Player p) {
		if(p.getGameMode() == GameMode.CREATIVE)
			return true;
		for(int i = 0; i < flyPerms.size(); i++) {
			if(flyPerms.get(i).equals(p.getName()))
				return true;
		}
		return false;
	}
	
	/*Checks whether a number is an int or not
	 * @return boolean, true if the number is an int
	 */
	public boolean isInt(String test) {
		try{
			Integer.parseInt(test);
			return true;
		}catch(NumberFormatException e) { return false; }
	}
	
	///**Let players know that fly doesn't work, and when this plugin
	 //* is installed, it needs to be substituted by /zerogflyperms add playername
	 //*/
	/*@EventHandler 
	public void flyEssentialsNoWork(PlayerCommandPreprocessEvent e) {
		String msg = e.getMessage();
		if(msg.length() > 4)
			msg = msg.substring(0, 4);
		if(msg.equalsIgnoreCase("/fly")) {
			e.getPlayer().sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.LIGHT_PURPLE +
					"ZeroSuit" + ChatColor.DARK_GRAY + "]:  " + ChatColor.BLUE +
					"/fly will not work because you have ZeroSuit plugin installed.  Instead use " +
					ChatColor.RED + "/zerogflyperms <_add | remove_> <playerName>" + ChatColor.BLUE +
					" to give flying permission.");
		}
	}*/
	
	private class Update implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			for(int i = 0; i < zs.size(); i++) {
				if(!zs.get(i).isOnline()) {
					zs.get(i).setAllowFlight(false);
					zs.get(i).setFlying(false);
					zs.remove(i);
					i--;
					continue;
				}
				boolean isInZero = false;
				for(int ii = 0; ii < zeroArea.size(); ii++) {
					if(isInZeroG(zs.get(i).getLocation(), zeroArea.get(ii))) 
						isInZero = true;
				}
				if(!isInZero && !hasFlyPerms(zs.get(i))) {
					zs.get(i).setAllowFlight(false);
					zs.get(i).setFlying(false);
					zs.remove(i);
					i--;
				}
				else if(isInZero) {
					zs.get(i).setAllowFlight(true);
					zs.get(i).setFlying(true);
				}
				else if(hasFlyPerms(zs.get(i))) {
					zs.get(i).setAllowFlight(true);
					zs.remove(i);
					i--;
				}
			}
		}
	}
	
}
