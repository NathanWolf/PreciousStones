package net.sacredlabyrinth.Phaed.PreciousStones.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;

import net.sacredlabyrinth.Phaed.PreciousStones.PreciousStones;
import net.sacredlabyrinth.Phaed.PreciousStones.field.Field;
import net.sacredlabyrinth.Phaed.PreciousStones.field.FieldFlag;
import net.sacredlabyrinth.Phaed.PreciousStones.managers.ForceFieldManager;

/**
 * 
 * @author RoinujNosde
 *
 */
public class WorldEditListener {
	
	@Subscribe
	public void onEditSession(EditSessionEvent event) throws WorldEditException {
		if (event.getStage() != Stage.BEFORE_CHANGE) {
			return;
		}
		
		if (event.getActor() == null) {
			return;
		}
		
		Actor actor = event.getActor();
		if (!actor.isPlayer()) {
			return;
		}
		
		Player player = Bukkit.getPlayer(actor.getUniqueId());
		event.setExtent(new ForceFieldProtectionExtent(player, BukkitAdapter.adapt(event.getWorld()), event.getExtent()));
	}
	
	public static class ForceFieldProtectionExtent extends AbstractDelegateExtent {
		
		private boolean allowed = true;
		private boolean warned = false;
		private boolean notified = false;
		private World world;
		private Player player;

		protected ForceFieldProtectionExtent(Player player, World world, Extent extent) {
			super(extent);
			this.player = player;
			this.world = world;
		}
		
		@Override
		public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 l, T block) throws WorldEditException {
			if (!allowed) {
				return allowed;
			}

			Location bukkitLocation = new Location(world, l.getX(), l.getY(), l.getZ());
			Block bukkitBlock = bukkitLocation.getBlock();
			
			PreciousStones ps = PreciousStones.getInstance();
			ForceFieldManager ffm = ps.getForceFieldManager();			
			
			Field field = ffm.getEnabledSourceField(bukkitLocation, FieldFlag.PREVENT_DESTROY);
			
			if (ffm.isField(bukkitBlock)) {
				return false;
			}
			
	        if (field != null) {
	            if (!field.getSettings().inDestroyBlacklist(bukkitBlock)) {
	                if (FieldFlag.PREVENT_DESTROY.applies(field, player)) {
	                    if (ps.getPermissionsManager().has(player, "preciousstones.bypass.destroy")) {
	                    	if (!notified) {
	                    		ps.getCommunicationManager().notifyBypassDestroy(player, bukkitBlock, field);
	                    		notified = true;
	                    	}
	                    } else {
	                    	allowed = false;
	                    	if (!warned) {
	                    		ps.getCommunicationManager().warnDestroyArea(player, bukkitBlock, field);
	                    		warned = true;
	                    	}
	                        return false;
	                    }
	                }
	            }
	        }
			
            return super.setBlock(l, block);
		}
	}
}
