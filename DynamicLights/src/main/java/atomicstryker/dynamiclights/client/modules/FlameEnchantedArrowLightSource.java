package atomicstryker.dynamiclights.client.modules;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import atomicstryker.dynamiclights.client.DynamicLights;
import atomicstryker.dynamiclights.client.IDynamicLightSource;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.lang3.ArrayUtils;

/**
 * 
 * @author AtomicStryker
 *
 * Offers Dynamic Light functionality to flame enchanted Arrows fired.
 * Those can give off Light through this Module.
 *
 */
@Mod(modid = "DynamicLights_flameArrows", name = "Dynamic Lights on Flame enchanted Arrows", version = "1.0.0", dependencies = "required-after:DynamicLights")
public class FlameEnchantedArrowLightSource
{

    private int[] disabledDimensions;



    @EventHandler
    public void preInit(FMLPreInitializationEvent evt)
    {

        Configuration config = new Configuration(evt.getSuggestedConfigurationFile());
        config.load();


        Property disabledDimensionIds = config.get(Configuration.CATEGORY_GENERAL, "disabled dimension ids",new int[] {-100});
        disabledDimensionIds.comment = "list of dimensions ids that are disabled";
        disabledDimensions = disabledDimensionIds.getIntList();

        config.save();

    }
    
    @EventHandler
    public void load(FMLInitializationEvent evt)
    {
        MinecraftForge.EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void onEntityJoinedWorld(EntityJoinWorldEvent event)
    {
        if (event.entity instanceof EntityArrow && !ArrayUtils.contains(disabledDimensions,event.entity.dimension))
        {
            EntityArrow arrow = (EntityArrow) event.entity;
            if (arrow.shootingEntity != null && arrow.shootingEntity instanceof EntityPlayer)
            {
                EntityPlayer shooter = (EntityPlayer) arrow.shootingEntity;
                if (EnchantmentHelper.getFireAspectModifier(shooter) != 0)
                {
                    DynamicLights.addLightSource(new EntityLightAdapter(arrow));
                }
            }
        }
    }
    
    private class EntityLightAdapter implements IDynamicLightSource
    {
        private EntityArrow entity;
        private int lightLevel;
        
        public EntityLightAdapter(EntityArrow entArrow)
        {
            lightLevel = 15;
            entity = entArrow;
        }
     
        @Override
        public Entity getAttachmentEntity()
        {
            return entity;
        }

        @Override
        public int getLightLevel()
        {
            return lightLevel;
        }
    }

}
