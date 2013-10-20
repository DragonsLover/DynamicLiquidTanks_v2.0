package doc.mods.dynamictanks.tileentity;

import java.util.Arrays;
import java.util.LinkedList;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet132TileEntityData;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import doc.mods.dynamictanks.api.IPowerEater;
import doc.mods.dynamictanks.api.IStorageUnit;
import doc.mods.dynamictanks.api.PowerController;

public class ControllerTileEntity extends CountableTileEntity implements IFluidHandler, IPowerEater, IStorageUnit {

	/*
	 * list vars
	 */	
	protected LinkedList<FluidTank> containedLiquids = new LinkedList<FluidTank>();
	protected LinkedList<int[]> neighborLocations = new LinkedList<int[]>(); 

	
	/*
	 * misc vars
	 */
	protected int[] camoMeta = { -1, -1 }; // first var camo, second var meta	
	
	protected double BONUS_MULT = 1.05;
	protected int INTERNAL_SIZE = 16;
	
	
	/*
	 * liquid vars
	 */
	protected int numLiquids = 1;
	protected int tankCapacity = INTERNAL_SIZE * FluidContainerRegistry.BUCKET_VOLUME;
	protected int toExtractFromTank = 0;

	private int maxNumAllowed = 6;
	
	/*
	 * Power Vars
	 */
	
	protected PowerController powerController = null;
	
	/* 
	 * Upgrade Variables
	 */

	protected double upgradeMult = 1.1;
	protected int powerOf = 0;
	
	/* 
	 * Commandsline commands/GUI Information
	 */
	
	public String[] validCommands = { 
			"getFluidCapacity", "getFluidAmount",
			"getStoredFluids", "getStoredFluidsCount", "emptyFluidToContainer",
			"grabFluid", "storeFluidFromInventory",
			"storeAllFluidFromInventory", "fluidsInfo", "setCamo",
			"setCamoWithMeta", "information" 
	};

	public ControllerTileEntity() {
		containedLiquids.add(new FluidTank(getTankCapacity()));
		
		powerController = new PowerController();
		powerController.modify(0, 6000, 15);
	}

	/* 
	 * Setters
	 */

	public void addAdditionalTank(int numToAdd) {
		for (int i = 0; i < numToAdd; i++)
			containedLiquids.add(new FluidTank(getTankCapacity()));
		numLiquids += numToAdd;
	}

	public boolean addNeighbor(int[] loc) {
		for(int i = 0; i < neighborLocations.size(); i++)
			if (Arrays.equals(loc, neighborLocations.get(i)))
				return false;
		
		neighborLocations.add(loc);
		return true;
	}

	public void resizeLiqInventory(int newSize) {
		this.numLiquids = newSize;
	}

	public void setCamo(int blockID) {
		camoMeta[0] = blockID;
	}

	public void setCamo(int blockID, int meta) {
		camoMeta[0] = blockID;
		camoMeta[1] = meta;
	}

	public void resizeTank(int newSize) {
		for (FluidTank fluidTank : containedLiquids) {
			fluidTank.setCapacity(newSize * FluidContainerRegistry.BUCKET_VOLUME);
		}
	}

	/*
	 * Getters
	 */

	public FluidTank getTankObj(FluidStack fluidStack) {
		for (FluidTank tank : containedLiquids) {
			if (tank.getFluid().isFluidEqual(fluidStack))
				return tank;
		}

		return null;
	}

	public int getTankCapacity() {
		return this.tankCapacity;
	}

	public int getAlwdLiquids() {
		return numLiquids;
	}

	public int getTotalAmount() {
		int amount = 0;
		for (FluidTank tank : containedLiquids) {
			amount += tank.getFluidAmount();
		}
		return amount;
	}
	
	public LinkedList<int[]> getNeighbors() {
		return neighborLocations;
	}

	/*
	 * Misc Methods
	 */
	public void nextLiquidIndex() {
		if (toExtractFromTank + 1 > containedLiquids.size()) {
			toExtractFromTank = 0;
			return;
		}
		toExtractFromTank++;
	}
	
	public void refreshTankCapacity() {
		for (FluidTank fluidTank : containedLiquids)
			fluidTank.setCapacity(getTankCapacity());
	}

	public void refresh() {
		int newCap = (int) (((neighborLocations.size() * INTERNAL_SIZE) * (BONUS_MULT)) * (Math.pow(upgradeMult, powerOf)) * FluidContainerRegistry.BUCKET_VOLUME);
		tankCapacity = newCap < (INTERNAL_SIZE * FluidContainerRegistry.BUCKET_VOLUME) ? INTERNAL_SIZE * FluidContainerRegistry.BUCKET_VOLUME : newCap;
		refreshTankCapacity();
	}

	/*
	 * TileEntity Methods
	 */
	@Override
	public void updateEntity() {		
		if (worldObj.isRemote) { //client side
			
		}
		
		if (!worldObj.isRemote) { //server side
			doCount();

			if (countMet()) { //perform events every maxTickCount
				refresh(); //resize capacity of FluidTank Array
				
				System.out.println("Number of Liquids: " + this.numLiquids);
				System.out.println("Capacity: " + this.tankCapacity);
				System.out.println("Tanks: " + this.neighborLocations.size());
				System.out.println("Contained Liquids: " + this.containedLiquids.size());
			}
		}
	}

	/*
	 * Syncing Methods
	 */

	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);

		numLiquids = tagCompound.getInteger("numLiquids");
		tankCapacity = tagCompound.getInteger("tankCapacity");
		camoMeta[0] = tagCompound.getInteger("blockID");
		camoMeta[1] = tagCompound.getInteger("meta");

		for (int i = 0; i < numLiquids; i++)
			containedLiquids.add(new FluidTank(new FluidStack(tagCompound.getInteger("itemID" + i), tagCompound.getInteger("amount" + i)), getTankCapacity()));
	}

	@Override
	public void writeToNBT(NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);

		tagCompound.setInteger("numLiquids", numLiquids);
		tagCompound.setInteger("tankCapacity", tankCapacity);
		tagCompound.setInteger("blockID", camoMeta[0]);
		tagCompound.setInteger("meta", camoMeta[1]);

		for (int i = 0; i < containedLiquids.size(); i++) {
			if (containedLiquids.get(i).getFluid() != null) {
				tagCompound.setInteger("itemID" + i, containedLiquids.get(i).getFluid().fluidID);
				tagCompound.setInteger("amount" + i, containedLiquids.get(i).getFluid().amount);
			}				
		}


	}

	@Override
	public Packet getDescriptionPacket () {
		NBTTagCompound tag = new NBTTagCompound();
		writeToNBT(tag);
		return new Packet132TileEntityData(xCoord, yCoord, zCoord, 1, tag);
	}

	@Override
	public void onDataPacket (INetworkManager net, Packet132TileEntityData packet) {
		readFromNBT(packet.customParam1);
	}

	/*
	 * ITankHandler
	 */

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		if (resource == null)
			return 0;

		resource = resource.copy();
		int totalUsed = 0;
		FluidTank tankToFill = null;

		for (FluidTank fluidTank : containedLiquids) 
			if (fluidTank.getFluid() != null && fluidTank.getFluid().isFluidEqual(resource))
				tankToFill = fluidTank;

		if (tankToFill == null)
			for (FluidTank fluidTank : containedLiquids)
				if (fluidTank.getFluid() == null) {
					tankToFill = fluidTank;
					break;
				}

		if (tankToFill == null)
			return 0;

		FluidStack liquid = tankToFill.getFluid();
		if (liquid != null && liquid.amount > 0 && !liquid.isFluidEqual(resource)) {
			return 0;
		}

		while (tankToFill != null && resource.amount > 0 && tankToFill.getFluidAmount() + resource.amount <= tankToFill.getCapacity()) {
			int used = tankToFill.fill(resource, doFill);
			resource.amount -= used;
			if (used > 0) {
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
			totalUsed += used;
		}

		return totalUsed;
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		for (FluidTank fluidTank : containedLiquids)
			if (fluidTank.getFluid().isFluidEqual(resource))
				return fluidTank.drain(resource.amount, doDrain);

		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		if (containedLiquids.isEmpty())
			return null;

		return containedLiquids.get(toExtractFromTank).drain(maxDrain, doDrain);
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return true;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return true;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return null;
	}

	
	/*
	 * @IPowerEater
	 */
	
	@Override
	public int consume() {
		// TODO Auto-generated method stub
		return 0;
	}

	
	/*
	 * @IStorageUnit
	 */
	
	@Override
	public int fillUnit() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int drainUnit() {
		// TODO Auto-generated method stub
		return 0;
	}
}