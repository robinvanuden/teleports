package com.varestorm.plugins.teleports;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.entity.Player;

import java.util.*;

public class TeleportList {

    public static final int MAX = 5;
    public static final int COOLDOWN = 10;

    protected final UUID uuid;
    protected Location home, bed, anchor, back;
    protected final Map<String, Location> list;
    protected final List<UUID> requests;
    protected long anchor_cooldown = 0, cross_world_cooldown = 0;

    public TeleportList(Player player) {
        this.uuid = player.getUniqueId();
        this.list = new HashMap<>();
        this.requests = new ArrayList<>();
    }

    public void sout() {
        System.out.println(locToString("home", this.home));
        System.out.println(locToString("back", this.back));
        System.out.println(locToString("bed", this.bed));
        System.out.println(locToString("anchor", this.anchor));
        System.out.println("list");
        if (this.list.size() > 0) {
            for (Map.Entry<String, Location> entry : this.list.entrySet()) {
                System.out.println(locToString(entry.getKey(), entry.getValue()));
            }
        }
    }

    public boolean hasHome() {
        return (this.home != null);
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public Location getHome() {
        return this.home;
    }

    public Location getBack() {
        return this.back;
    }

    public void setBack(Location back) {
        this.back = back;
    }

    public Location getBed() {
        return this.bed;
    }

    public void setBed(Location bed) {
        this.bed = bed;
    }

    public boolean hasNoBed() {
        if (this.bed == null) {
            return true;
        }
        return !this.bed.getBlock().getBlockData().getMaterial().name().contains("_BED");
    }

    public boolean isYourBed(Location location) {
        if (location == null) {
            return false;
        }
        if (this.hasNoBed()) {
            return false;
        }
        if (location.equals(this.bed)) {
            return true;
        }
        return this.getOtherBedBlock().equals(location);
    }

    public boolean hasNoAnchor() {
        if (this.anchor == null) {
            return true;
        }
        return this.anchor.getBlock().getBlockData().getMaterial() != Material.RESPAWN_ANCHOR;
    }

    public boolean isYourAnchor(Location location) {
        if (location == null) {
            return false;
        }
        if (this.hasNoAnchor()) {
            return false;
        }
        return location.equals(this.anchor);
    }

    public RespawnAnchor getAnchorBlock() {
        if (this.hasNoAnchor()) {
            return null;
        }
        Block anchor_block = this.anchor.getBlock();
        BlockData anchor_data = anchor_block.getBlockData();

        if (anchor_data instanceof RespawnAnchor) {
            return (RespawnAnchor) anchor_data;
        }
        return null;
    }

    public boolean isAnchorCharged() {
        return (this.getAnchorCharges() > 0);
    }

    public int getAnchorCharges() {
        RespawnAnchor anchor = this.getAnchorBlock();
        if (anchor == null) {
            return 0;
        }
        return anchor.getCharges();
    }

    public Location getOtherBedBlock() {
        if (this.bed == null) {
            return null;
        }
        Block bed_block = this.bed.getBlock();
        BlockData data = bed_block.getBlockData();

        if (data instanceof Bed) {
            Bed bed = (Bed) data;
            if (bed.getPart() == Bed.Part.FOOT) {
                return bed_block.getRelative(bed.getFacing()).getLocation();
            } else {
                return bed_block.getRelative(bed.getFacing().getOppositeFace()).getLocation();
            }
        }
        return null;
    }

    public Location getAnchor() {
        return this.anchor;
    }

    public void setAnchor(Location anchor) {
        this.anchor = anchor;
    }

    public void set(String name, Location location) {
        this.list.put(name, location);
    }

    public Location get(String name) {
        return this.list.get(name);
    }

    public void remove(String name) {
        this.list.remove(name);
    }

    public Map<String, Location> list() {
        return this.list;
    }

    public int count() {
        return this.list.size();
    }

    public int left() {
        return MAX - this.list.size();
    }

    public boolean canSave(String name) {
        int count = this.count();
        if (count < MAX) {
            return true;
        }
        return this.get(name) != null && count == MAX;
    }

    public void request(Player player) {
        this.requests.add(player.getUniqueId());
    }

    public List<UUID> requests() {
        return this.requests;
    }

    public void decline() {
        this.requests.clear();
    }

    public static String locToString(String name, Location l) {
        if (l != null) {
            return String.format("%s:%s,%s,%s,%s,%s,%s", name, Objects.requireNonNull(l.getWorld()).getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        }
        return "";
    }

    public static String locToString(Location l) {
        if (l != null) {
            return String.format("%s,%s,%s,%s,%s,%s", Objects.requireNonNull(l.getWorld()).getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch());
        }
        return "";
    }

    public static Location stringToLoc(Application application, String s) {
        if (s.contains(",")) {
            String[] split = s.split(",");
            World world = application.getServer().getWorld(split[0]);
            if (world != null) {
                double x = Double.parseDouble(split[1]);
                double y = Double.parseDouble(split[2]);
                double z = Double.parseDouble(split[3]);
                float yaw = Float.parseFloat(split[4]);
                float pitch = Float.parseFloat(split[5]);
                return new Location(world, x, y, z, yaw, pitch);
            }
        }
        return null;
    }

    public JsonObject toJSON() {
        JsonObject json = new JsonObject();
        json.addProperty("home", locToString(this.home));
        json.addProperty("back", locToString(this.back));
        json.addProperty("bed", locToString(this.bed));
        json.addProperty("anchor", locToString(this.anchor));
        JsonObject custom = new JsonObject();
        for (Map.Entry<String, Location> entry : this.list.entrySet()) {
            custom.addProperty(entry.getKey(), locToString(entry.getValue()));
        }
        json.add("custom", custom);
        return json;
    }

    public void fromJson(Application application, JsonObject object) {
        if (object != null) {
            this.home = stringToLoc(application, object.get("home").getAsString());
            this.back = stringToLoc(application, object.get("back").getAsString());
            this.bed = stringToLoc(application, object.get("bed").getAsString());
            this.anchor = stringToLoc(application, object.get("anchor").getAsString());
            JsonObject custom = object.get("custom").getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : custom.entrySet()) {
                this.list.put(entry.getKey(), stringToLoc(application, entry.getValue().getAsString()));
            }
        }
    }

    public boolean canClickAnchor() {
        long now = new Date().getTime();
        long left = now - this.anchor_cooldown;
        if (left < 0) {
            this.anchor_cooldown = now;
            return true;
        }
        if (left > 200) {
            this.anchor_cooldown = now;
            return true;
        }
        return false;
    }

    public int getCrossTeleportCooldown() {
        long now = new Date().getTime();
        long left = now - this.cross_world_cooldown;
        return (int) (COOLDOWN - Math.max(left / 1000, 0));
    }

    public boolean canCrossTeleport() {
        long now = new Date().getTime();
        long left = now - this.cross_world_cooldown;
        if (left < 0) {
            this.cross_world_cooldown = now;
            return true;
        }
        if (left > (COOLDOWN * 1000)) {
            this.cross_world_cooldown = now;
            return true;
        }
        return false;
    }
}
