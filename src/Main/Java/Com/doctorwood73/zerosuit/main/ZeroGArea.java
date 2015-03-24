package com.doctorwood73.zerosuit.main;

import java.io.Serializable;

import org.bukkit.ChatColor;

/**This class holds objects of the locations of set areas
 * where there are zero g's*/
public class ZeroGArea implements Serializable {
	/**Holds the 2 block locations from which everything inbetween
	 * will be labeled as a zero-g area*/
	public int x1;
	public int y1;
	public int z1;
	public int x2;
	public int y2;
	public int z2;
	
	/**The world environment that is zero g*/
	public String world;
	
	/**Name ID of the area*/
	public String nameID;
	
	/**Initializes the object's attributes*/
	public ZeroGArea(int x1, int y1, int z1, int x2, int y2, int z2, String world, String nameID) {
		this.x1=x1;
		this.y1=y1;
		this.z1=z1;
		this.x2=x2;
		this.y2=y2;
		this.z2=z2;
		this.world=world;
		this.nameID=nameID;
	}
	
	
	
	/**Returns the data of this instance
	 * @return String well presented data of this instance
	 */
	public String toString() {
		return ChatColor.LIGHT_PURPLE + nameID + ChatColor.RED + ":" + 
				ChatColor.GOLD + "Corner 1: " + ChatColor.AQUA +
				"\n(" + String.valueOf(x1) + ", " + String.valueOf(y1) + ", " +
				String.valueOf(z1) + ")\n" + ChatColor.GOLD + "Corner 2: " + ChatColor.AQUA +
				"(" + String.valueOf(x2) + ", " + String.valueOf(y2) + ", " + String.valueOf(z2);
	}
	
	/**Getter method for the ID*/
	public String getID() {
		return nameID;
	}
	
	/**Getter method for the world*/
	public String getWorld() {
		return world;
	}
}
