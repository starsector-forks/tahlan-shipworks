package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import data.scripts.campaign.bar.hunt_for_kassadar.tahlan_HuntForKassadarBarEventCreator;
import data.scripts.campaign.sectorgen.tahlan_GenerateKassadarScript;
import org.dark.shaders.light.LightData;
import org.dark.shaders.util.ShaderLib;
import org.dark.shaders.util.TextureData;

public class tahlan_ModPlugin extends BaseModPlugin {//Stores if we use ShaderLib; enables some fancier functions to be used down-the-line since we're tracking it
    static private boolean graphicsLibAvailable = false;
    static public boolean isGraphicsLibAvailable () {
        return graphicsLibAvailable;
    }

    //Application loading stuff; mostly compatibility checks
    @Override
    public void onApplicationLoad() {
        boolean hasLazyLib = Global.getSettings().getModManager().isModEnabled("lw_lazylib");
        if (!hasLazyLib) {
            throw new RuntimeException("Tahlan Shipworks requires LazyLib by LazyWizard");
        }
        boolean hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
        if (!hasMagicLib) {
            throw new RuntimeException("Tahlan Shipworks requires MagicLib!");
        }
        boolean hasSSFX = Global.getSettings().getModManager().isModEnabled("xxx_ss_FX_mod");
        if (hasSSFX) {
            throw new RuntimeException("Tahlan Shipworks is not compatible with Starsector FX");
        }

        boolean hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            graphicsLibAvailable = true;
            ShaderLib.init();
            LightData.readLightDataCSV("data/lights/tahlan_lights.csv");
            TextureData.readTextureDataCSV("data/lights/tahlan_texture.csv");
        } else {
            graphicsLibAvailable = false;
        }
    }

    //New game stuff
    @Override
    public void onNewGame() {
        //Prevents Vendetta (GH) from appearing in fleets unless DaRa is installed
        if (Global.getSector().getFaction("tahlan_greathouses") != null) {
            if (!Global.getSettings().getModManager().isModEnabled("DisassembleReassemble")) {
                Global.getSector().getFaction("tahlan_greathouses").removeKnownShip("tahlan_vendetta_gh");
                Global.getSector().getFaction(Factions.INDEPENDENT).removeKnownShip("tahlan_vendetta_gh");
            }
        }

        //Generates Kassadar into the sector
        tahlan_GenerateKassadarScript.generate(Global.getSector());
    }


    //Load-game stuff. This is currently only used to ensure all our bar events appear even in old saves
    @Override
    public void onGameLoad(boolean newGame) {
        //Adds the Hunt for Kassadar bar event, if the event manager doesn't already have it
        BarEventManager bar = BarEventManager.getInstance();
        if (!bar.hasEventCreator(tahlan_HuntForKassadarBarEventCreator.class)) {
            bar.addEventCreator(new tahlan_HuntForKassadarBarEventCreator());
        }
    }
}
