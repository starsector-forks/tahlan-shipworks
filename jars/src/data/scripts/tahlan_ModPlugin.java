package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

public class tahlan_ModPlugin extends BaseModPlugin {

    //Application loading stuff; mostly compatibility checks
    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Tahlan Shipworks requires LazyLib by LazyWizard");
        }
        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasMagicLib) {
            throw new RuntimeException("Tahlan Shipworks requires MagicLib!");
        }
        boolean hasSSFX = Global.getSettings().getModManager().isModEnabled("xxx_ss_FX_mod");
        if (hasSSFX) {
            throw new RuntimeException("Tahlan Shipworks is not compatible with Starsector FX");
        }

        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/tahlan_lights.csv");
            TextureData.readTextureDataCSV("data/lights/tahlan_texture.csv");
        }

    }

    //New game stuff. Currently, it's just to prevent the Vendetta (GH) from spawning in fleets without the correct mod
    @Override
    public void onNewGame() {
        if (Global.getSettings().getModManager().isModEnabled("DisassembleReassemble")) {
            Global.getSector().getFaction("tahlan_greathouses").addKnownShip("tahlan_vendetta_gh", true);
        }
    }
}
