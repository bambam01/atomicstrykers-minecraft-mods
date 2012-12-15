package ic2.advancedmachines.common;

import ic2.api.Direction;
import ic2.api.NetworkHelper;

import java.util.List;

import net.minecraft.src.Container;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.common.ISidedInventory;

public abstract class TileEntityAdvancedMachine extends TileEntityBaseMachine implements ISidedInventory
{
    private static final int MAX_PROGRESS = 4000;
    private static final int MAX_ENERGY = 5000;
    private static final int MAX_SPEED = 7500;
    private static final int MAX_INPUT = 32;
    private String inventoryName;
    private int[] inputs;
    private int[] outputs;
    short speed;
    short progress;
    private String dataFormat;
    private int dataScaling;
    
    private IC2AudioSource audioSource;
    private static final int EventStart = 0;
    private static final int EventInterrupt = 1;
    private static final int EventStop = 2;
    
    private int energyConsume = 2;
    private int acceleration = 1;
    private int maxSpeed;

    public TileEntityAdvancedMachine(String invName, String dataForm, int dataScale, int[] inputSlots, int[] outputSlots)
    {
        super(inputSlots.length + outputSlots.length + 6, MAX_ENERGY, MAX_INPUT);
        this.inventoryName = invName;
        this.dataFormat = dataForm;
        this.dataScaling = dataScale;
        this.inputs = inputSlots;
        this.outputs = outputSlots;
        this.speed = 0;
        this.progress = 0;
    }

    @Override
    public void readFromNBT(NBTTagCompound var1)
    {
        super.readFromNBT(var1);
        this.speed = var1.getShort("speed");
        this.progress = var1.getShort("progress");
    }

    @Override
    public void writeToNBT(NBTTagCompound var1)
    {
        super.writeToNBT(var1);
        var1.setShort("speed", this.speed);
        var1.setShort("progress", this.progress);
    }

    @Override
    public String getInvName()
    {
        return this.inventoryName;
    }

    public int gaugeProgressScaled(int var1)
    {
        return var1 * this.progress / MAX_PROGRESS;
    }

    public int gaugeFuelScaled(int var1)
    {
        return var1 * this.energy / this.maxEnergy;
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();
        
        if (worldObj.isRemote)
        {
            return;
        }
        
        boolean newItemProcessing = false;
        if (this.energy <= this.maxEnergy)
        {
            newItemProcessing = this.provideEnergy();
        }

        boolean isActive = this.getActive();
        if (this.progress >= MAX_PROGRESS)
        {
            this.operate();
            newItemProcessing = true;
            this.progress = 0;
            isActive = false;
            
            NetworkHelper.initiateTileEntityEvent(this, EventStop, true);
        }

        boolean bCanOperate = this.canOperate();
        if (this.energy > 0 && (bCanOperate || this.isRedstonePowered()))
        {
        	setOverclockRates();
        	
            if (this.speed < maxSpeed)
            {
                this.speed += acceleration;
                this.energy -= energyConsume;
            }
            else
            {
            	this.speed = (short) maxSpeed;
            	this.energy -= AdvancedMachines.defaultEnergyConsume;
            }

            isActive = true;
            NetworkHelper.initiateTileEntityEvent(this, EventStart, true);
        }
        else
        {
        	boolean wasWorking = this.speed != 0;
            this.speed = (short)(this.speed - Math.min(this.speed, 4));
            if (wasWorking && this.speed == 0)
            {
            	NetworkHelper.initiateTileEntityEvent(this, EventInterrupt, true);
            }
        }

        if (isActive && this.progress != 0)
        {
            if (!bCanOperate || this.speed == 0)
            {
                if (!bCanOperate)
                {
                    this.progress = 0;
                }

                isActive = false;
            }
        }
        else if (bCanOperate)
        {
            if (this.speed != 0)
            {
                isActive = true;
            }
        }
        else
        {
            this.progress = 0;
        }

        if (isActive && bCanOperate)
        {
            this.progress = (short)(this.progress + this.speed / 30);
        }

        if (newItemProcessing)
        {
            this.onInventoryChanged();
        }
        /*
        if(isActive != getActive())
    	{
    		worldObj.markBlockNeedsUpdate(xCoord, yCoord, zCoord);
    		setActive(isActive);
    	}
    	*/
    }
    
    @Override
    public int injectEnergy(Direction var1, int var2)
    {
    	this.setOverclockRates();
		return super.injectEnergy(var1, var2);
    }

    private void operate()
    {
        if (this.canOperate())
        {
            //ItemStack resultStack = this.getResultFor(this.inventory[this.inputs[this.inputs[0]]], false).copy();
			ItemStack resultStack = this.getResultFor(this.inventory[inputs[0]], false);
            int[] resultingStackSizesOutputs = new int[outputs.length];
            int resultMaxStackSize = resultStack.getMaxStackSize();

            int index;
            for (index = 0; index < outputs.length; ++index)
            {
                if (inventory[outputs[index]] == null)
                {
                    resultingStackSizesOutputs[index] = resultMaxStackSize;
                }
                else if (inventory[outputs[index]].isItemEqual(resultStack))
                {
                    resultingStackSizesOutputs[index] = resultMaxStackSize - inventory[outputs[index]].stackSize;
                }
            }

            for (index = 0; index < resultingStackSizesOutputs.length; ++index)
            {
                if (resultingStackSizesOutputs[index] > 0)
                {
                    int resultingStackSize = Math.min(resultStack.stackSize, resultingStackSizesOutputs[index]);
                    if (this.inventory[this.outputs[index]] == null)
                    {
                        this.inventory[this.outputs[index]] = getResultFor(this.inventory[this.inputs[0]], true);
                        onFinishedProcessingItem();
                    }
                    else
                    {
                        getResultFor(this.inventory[this.inputs[0]], true);
                        this.inventory[this.outputs[index]].stackSize += resultingStackSize;
                        onFinishedProcessingItem();
                    }

                    resultStack.stackSize -= resultingStackSize;
                }

                if (resultStack.stackSize <= 0)
                {
                    break;
                }
            }

            if (this.inventory[this.inputs[0]].stackSize <= 0)
            {
                this.inventory[this.inputs[0]] = null;
            }
        }
    }
    
    public void onFinishedProcessingItem()
    {
    	
    }

    private boolean canOperate()
    {
        if (this.inventory[this.inputs[0]] == null)
        {
            return false;
        }
        else
        {
            ItemStack resultStack = this.getResultFor(this.inventory[this.inputs[0]], false);
            if (resultStack == null)
            {
                return false;
            }
            else
            {
                int resultMaxStackSize = resultStack.getMaxStackSize();
                int resultingStackSize = 0;
                int[] outputArray = this.outputs;
                int outputsLength = outputArray.length;

                for (int index = 0; index < outputsLength; ++index)
                {
                    int curOutputSlot = outputArray[index];
                    if (this.inventory[curOutputSlot] == null)
                    {
                        resultingStackSize += resultMaxStackSize;
                    }
                    else if (this.inventory[curOutputSlot].isItemEqual(resultStack))
                    {
                        resultingStackSize += resultMaxStackSize - this.inventory[curOutputSlot].stackSize;
                    }
                }

                return resultingStackSize >= resultStack.stackSize;
            }
        }
    }

    public abstract ItemStack getResultFor(ItemStack input, boolean adjustOutput);

    protected abstract List getResultMap();

    public abstract Container getGuiContainer(InventoryPlayer var1);

    @Override
    public int getStartInventorySide(ForgeDirection side)
    {
        switch (side)
        {
            case DOWN:
                return 0; // power slot always 0
            case UP:
                return inputs[0];
            default:
                return outputs[0];
        }
    }

    @Override
    public int getSizeInventorySide(ForgeDirection side)
    {
        switch (side)
        {
            case DOWN:
                return 1;
            case UP:
                return this.inputs.length;
            default:
                return this.outputs.length;
        }
    }

    public String printFormattedData()
    {
        return String.format(this.dataFormat, new Object[] {Integer.valueOf(this.speed * this.dataScaling)});
    }
    
    @Override
    public void invalidate()
    {
    	if (this.audioSource != null)
    	{
    		IC2AudioSource.removeSource(audioSource);
    		this.audioSource = null;
    	}
    	super.invalidate();
    }
    
    protected String getStartSoundFile()
    {
    	return null;
    }

    protected String getInterruptSoundFile()
    {
    	return null;
    }

    @Override
    public void onNetworkEvent(int event)
    {
    	super.onNetworkEvent(event);
    	
    	if (worldObj.isRemote)
    	{
            if ((this.audioSource == null) && (getStartSoundFile() != null))
            {
                this.audioSource = new IC2AudioSource(this, getStartSoundFile());
            }

            switch (event)
            {
                case EventStart:
                    this.setActiveWithoutNotify(true);
                    if (this.audioSource == null) break;
                    this.audioSource.play();
                    break;
                case EventInterrupt:
                    this.setActiveWithoutNotify(false);
                    if (this.audioSource == null) break;
                    this.audioSource.stop();
                    if (getInterruptSoundFile() == null) break;
                    IC2AudioSource.playOnce(this, getInterruptSoundFile());
                    break;
                case EventStop:
                    this.setActiveWithoutNotify(false);
                    if (this.audioSource == null) break;
                    this.audioSource.stop();
            }
    	}
    	
    	NetworkHelper.announceBlockUpdate(worldObj, xCoord, yCoord, zCoord);
    }
    
    public abstract int getUpgradeSlotsStartSlot();
    
    public void setOverclockRates()
    {
    	int overclockerUpgradeCount = 0;
    	int transformerUpgradeCount = 0;
    	int energyStorageUpgradeCount = 0;

    	for (int i = 0; i < 4; i++) {
    		ItemStack itemStack = this.inventory[getUpgradeSlotsStartSlot() + i];

    		if (itemStack != null) {
    			if (itemStack.isItemEqual(AdvancedMachines.overClockerStack))
    				overclockerUpgradeCount += itemStack.stackSize;
    			else if (itemStack.isItemEqual(AdvancedMachines.transformerStack))
    				transformerUpgradeCount += itemStack.stackSize;
    			else if (itemStack.isItemEqual(AdvancedMachines.energyStorageUpgradeStack)) {
    				energyStorageUpgradeCount += itemStack.stackSize;
    			}
    		}
    	}

    	if (overclockerUpgradeCount > 32) overclockerUpgradeCount = 32;
    	if (transformerUpgradeCount > 10) transformerUpgradeCount = 10;

    	this.energyConsume = (int)(AdvancedMachines.defaultEnergyConsume * Math.pow(AdvancedMachines.overClockEnergyRatio, overclockerUpgradeCount));
    	this.acceleration = (int)(((AdvancedMachines.defaultAcceleration) * Math.pow(AdvancedMachines.overClockAccelRatio, overclockerUpgradeCount)) /2);
    	this.maxSpeed = (MAX_SPEED + overclockerUpgradeCount * AdvancedMachines.overClockSpeedBonus);
    	this.maxInput = (MAX_INPUT * (int)Math.pow(AdvancedMachines.overLoadInputRatio, transformerUpgradeCount));
    	this.maxEnergy = (MAX_ENERGY + energyStorageUpgradeCount * MAX_ENERGY + this.maxInput - 1);
    	this.tier = (transformerUpgradeCount);
    }
}
