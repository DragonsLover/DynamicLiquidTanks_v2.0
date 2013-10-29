package doc.mods.dynamictanks.client;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import doc.mods.dynamictanks.client.gui.GuiController;
import doc.mods.dynamictanks.common.CommonProxy;
import doc.mods.dynamictanks.tileentity.ControllerTileEntity;
import doc.mods.dynamictanks.tileentity.UpgradeTileEntity;

public class ClientProxy extends CommonProxy
{
	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity tileEntity = world.getBlockTileEntity(x, y, z);

		if (tileEntity instanceof UpgradeTileEntity)
			return null;
		
		if (tileEntity instanceof ControllerTileEntity)
			return new GuiController(player.inventory, (UpgradeTileEntity) tileEntity);

		return null;
	}
}
