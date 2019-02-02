//By Nicke535
//Generates Kassadar and properly stores all the data needed to access it from various quests and rules.csv
package data.scripts.campaign.sectorgen;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class tahlan_GenerateKassadarScript {
    //The memory keys to use when storing data about Kassadar, and some other stuff for rules.csv
    public static String KASSADAR_PLANET_KEY = "$tahlan_kassadar_planet";
    public static String KASSADAR_INTERACTION_TAG = "tahlan_kassadar_plane t_locked_tag";


    //An offset for calculating spawn probability; lower values mean that the system has a larger chance of appearing
    //close to the sector's center. CANNOT be 0; set to 1 at minimum
    private static final float DISTANCE_CALCULATION_OFFSET = 3000f;


    //Main generation function
    public static void generate(SectorAPI sector) {
        WeightedRandomPicker<StarSystemAPI> picker = new WeightedRandomPicker<>();
        for (StarSystemAPI system : sector.getStarSystems()) {
            //Ignore non-procgen systems
            if (!system.isProcgen()) {continue;}

            //Ignore non-single-star systems, they prove too difficult to add planets to
            if (!system.getType().equals(StarSystemGenerator.StarSystemType.SINGLE)) {continue;}

            //Also ignore systems with no stars, or with pulsars/black holes
            if (system.getStar() == null || system.getStar().getTypeId().equals(StarTypes.BLACK_HOLE) || system.getStar().getTypeId().equals(StarTypes.NEUTRON_STAR)) {
                continue;
            }

            //Also ignore remnant and populated systems
            if (system.getTags().contains(Tags.THEME_REMNANT) || system.getTags().contains(Tags.THEME_CORE_POPULATED)) {continue;}

            //Then, calculate a weight depending on distance
            float weight = 10000f / (DISTANCE_CALCULATION_OFFSET + system.getLocation().length());
            picker.add(system, weight);
        }

        //Now, simply pick a system from the picker and work with that
        StarSystemAPI system = picker.pick();

        //First, get an orbit around the star (or a gas giant) at random
        LinkedHashMap<BaseThemeGenerator.LocationType, Float> hashmap = new LinkedHashMap<>();
        hashmap.put(BaseThemeGenerator.LocationType.STAR_ORBIT, 1f);
        hashmap.put(BaseThemeGenerator.LocationType.GAS_GIANT_ORBIT, 0.4f);
        WeightedRandomPicker<BaseThemeGenerator.EntityLocation> potentialLocations = BaseThemeGenerator.getLocations(null, system,
                500f, hashmap);
        BaseThemeGenerator.EntityLocation pickedLocation = potentialLocations.pick();

        //Then, we generate Kassadar and put it on said orbit
        PlanetAPI kassadar = system.addPlanet("tahlan_kassadar", system.getCenter(), "Castrum", "desert", 75, 280, 10, 10);
        kassadar.setOrbit(pickedLocation.orbit);

        //Add a custom description, and generate the market
        kassadar.setCustomDescriptionId("tahlan_planet_kassadar");
        MarketAPI kassadar_market = addMarketplace(Factions.INDEPENDENT, kassadar, null,
                "Castrum", // name of the market
                4, // size of the market
                new ArrayList<>(
                        Arrays.asList(
                                Conditions.POPULATION_4,
                                Conditions.ORE_ABUNDANT,
                                Conditions.RARE_ORE_MODERATE,
                                Conditions.ORGANICS_TRACE,
                                Conditions.VOLATILES_DIFFUSE,
                                Conditions.FARMLAND_POOR,
                                Conditions.HOT,
                                Conditions.DENSE_ATMOSPHERE,
                                Conditions.HABITABLE)),
                new ArrayList<>(
                        Arrays.asList( // Which submarkets to generate
                                Submarkets.GENERIC_MILITARY,
                                Submarkets.SUBMARKET_OPEN,
                                "tahlan_great_house_refits", //Special submarket for building Great Houses ships
                                Submarkets.SUBMARKET_STORAGE)),
                new ArrayList<>(
                        Arrays.asList( // Which industries we have on the market
                                Industries.POPULATION,
                                Industries.LIGHTINDUSTRY,
                                Industries.MINING,
                                Industries.REFINING,
                                Industries.MEGAPORT,
                                Industries.BATTLESTATION_HIGH,
                                Industries.HEAVYBATTERIES,
                                Industries.ORBITALWORKS)),
                0.3f, // tariff amount
                false, // Free Port
                true); //Has junk and chatter

        //Hide the market, so it doesn't do stuff it's not supposed to, and so it doesn't interact with other markets
        kassadar_market.setEconGroup(kassadar_market.getId());

        //Then, add some memory hooks; we want to be able to find this thing later
        sector.getMemoryWithoutUpdate().set(KASSADAR_PLANET_KEY, kassadar);
        kassadar.getTags().add(KASSADAR_INTERACTION_TAG);
    }


    //Shorthand function for adding a market
    public static MarketAPI addMarketplace(String factionID, SectorEntityToken primaryEntity, ArrayList<SectorEntityToken> connectedEntities, String name,
                                           int size, ArrayList<String> marketConditions, ArrayList<String> submarkets, ArrayList<String> industries, float tarrif,
                                           boolean freePort, boolean withJunkAndChatter) {
        EconomyAPI globalEconomy = Global.getSector().getEconomy();
        String planetID = primaryEntity.getId();
        String marketID = planetID + "_market";

        MarketAPI newMarket = Global.getFactory().createMarket(marketID, name, size);
        newMarket.setFactionId(factionID);
        newMarket.setPrimaryEntity(primaryEntity);
        newMarket.getTariff().modifyFlat("generator", tarrif);

        //Adds submarkets
        if (null != submarkets) {
            for (String market : submarkets) {
                newMarket.addSubmarket(market);
            }
        }

        //Adds market conditions
        for (String condition : marketConditions) {
            newMarket.addCondition(condition);
        }

        //Add market industries
        for (String industry : industries) {
            newMarket.addIndustry(industry);
        }

        //Sets us to a free port, if we should
        newMarket.setFreePort(freePort);

        //Adds our connected entities, if any
        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                newMarket.getConnectedEntities().add(entity);
            }
        }

        globalEconomy.addMarket(newMarket, withJunkAndChatter);
        primaryEntity.setMarket(newMarket);
        primaryEntity.setFaction(factionID);

        if (null != connectedEntities) {
            for (SectorEntityToken entity : connectedEntities) {
                entity.setMarket(newMarket);
                entity.setFaction(factionID);
            }
        }

        //Finally, return the newly-generated market
        return newMarket;
    }
}
