package atomicstryker.ruins.common;


import cpw.mods.fml.common.Mod;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@Mod(modid = "AS_BareRuins", name = "BareRuins Mod", version = "1", dependencies = "after:ExtraBiomes")
public class RuinsMod implements IFMLLoadingPlugin
{


    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}