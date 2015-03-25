/*
 * Copyright 2015 CrystalCraftMC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.crystalcraftmc.zerosuit;

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;

/**
 * Main class:
 * This program will automatically enable fly-mode when someone enters an area
 *
 * @author Alex Woodrow
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
			if(p.hasPermission("ZeroSuit.zerog") && label.equalsIgnoreCase("zerog")) {
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
							if(zs.get(ii).equals(e.getPlayer().getName()))
									isInList = true;
						}
						if(!isInList)
							zs.add(e.getPlayer());
						e.getPlayer().setAllowFlight(true);
						e.getPlayer().setFlying(true);
						return;
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
			boolean gettoInZero = false;
			for(int i = 0; i < zeroArea.size(); i++) {
				if(!this.isInZeroG(e.getTo(), zeroArea.get(i))) {
					gettoInZero = true;
				}
			}
			if(!gettoInZero) {
				if(!hasFlyPerms(e.getPlayer())) {
					p.setFlying(false);
					p.setAllowFlight(false);
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
		if(file.exists())
			file.delete();
		if(!new File("ZeroSuitFiles").exists())
			new File("ZeroSuitFiles").mkdir();
	}
	
	
	/**Checks whether a player has permission to fly
	 * @return boolean, true if the player has permission to fly
	 */
	public boolean hasFlyPerms(Player p) {
		if(p.getGameMode() == GameMode.CREATIVE)
			return true;
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
	
	
	
	private class Update implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			for(int i = 0; i < zs.size(); i++) {
				if(!zs.get(i).isOnline()) {
					zs.get(i).setFlying(false);
					zs.get(i).setAllowFlight(false);
					zs.remove(zs.get(i));
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
