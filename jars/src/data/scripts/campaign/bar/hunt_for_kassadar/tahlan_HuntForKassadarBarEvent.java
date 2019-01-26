//Originally by Inventor Raccoon, heavily modified by Nicke535
package data.scripts.campaign.bar.hunt_for_kassadar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.FullName.Gender;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.AddedEntity;
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator.EntityLocation;
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.ShipRecoverySpecial;
import com.fs.starfarer.api.impl.campaign.terrain.DebrisFieldTerrainPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.awt.*;

import static data.scripts.campaign.sectorgen.tahlan_GenerateKassadarScript.KASSADAR_PLANET_KEY;

public class tahlan_HuntForKassadarBarEvent extends BaseBarEventWithPerson {

	//Handles our various memory keys we will be using later
	public static String PROMISE_MINION_KEY = "$promise_Minion";
	public static String PROMISE_SYSTEM1_KEY = "$promise_system1";
	public static String PROMISE_SYSTEM2_KEY = "$promise_system2";

	//The different stages of the dialogue
	public static enum OptionId {
		INIT,
		TELL_STORY,
		CONTINUE_1,
		CONTINUE_2,
		CONTINUE_3,
		LEAVE,
	}

	// get the frigate's entity
	public static AddedEntity getOmicronMinion() {
		return (AddedEntity) Global.getSector().getMemoryWithoutUpdate().get(PROMISE_MINION_KEY);
	}

	// get Omicron's entity
	public static AddedEntity getOmicron() {
		return (AddedEntity) Global.getSector().getMemoryWithoutUpdate().get(KASSADAR_PLANET_KEY);
	}

	// get the frigate's system location
	public static StarSystemAPI getSystem1() {
		return (StarSystemAPI) Global.getSector().getMemoryWithoutUpdate().get(PROMISE_SYSTEM1_KEY);
	}

	// get Omicron's system location
	public static StarSystemAPI getSystem2() {
		return (StarSystemAPI) Global.getSector().getMemoryWithoutUpdate().get(PROMISE_SYSTEM2_KEY);
	}
	
	public tahlan_HuntForKassadarBarEvent() {
		super();
	}

	//Determines where the bar event will show up
	public boolean shouldShowAtMarket(MarketAPI market) {
		//First check the default implementation
		if (!super.shouldShowAtMarket(market)) return false;

		//Some factions can't get the event to spawn in their markets
		if (market.getFactionId().equals(Factions.LUDDIC_PATH) || market.getFactionId().equals(Factions.TRITACHYON)) {
			return false;
		}

		//The player can only find the event after reaching a certain level (or when debug is turned on)
		if (Global.getSector().getPlayerStats().getLevel() < 9 && !DebugFlags.BAR_DEBUG) return false;
		
		return true;
	}


	protected void generate() {

		WeightedRandomPicker<StarSystemAPI> picker1 = new WeightedRandomPicker<StarSystemAPI>(random);
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (Misc.getMarketsInLocation(system).isEmpty()) continue;

			picker1.add(system);
		}

		WeightedRandomPicker<StarSystemAPI> picker2 = new WeightedRandomPicker<StarSystemAPI>(random);
		for (StarSystemAPI system : Global.getSector().getStarSystems()) {
			if (system.hasTag(Tags.THEME_CORE_POPULATED) || system.hasTag(Tags.THEME_CORE_UNPOPULATED)) continue;
			float w = 1f;
			if (system.hasTag(Tags.THEME_RUINS_SECONDARY)) {
				w = 10000f;
			} else if (system.hasTag(Tags.THEME_RUINS_MAIN)) {
				w = 30000f;
			}

			//if (!Misc.getMarketsInLocation(system).isEmpty()) continue;

			picker2.add(system);
		}

		StarSystemAPI system1 = picker1.pick();
		//if (system1 == null) {
		//	doDataFail();
		//	return;
		//}

		StarSystemAPI system2 = picker2.pick();
		//if (system2 == null) {
		//	doDataFail();
		//	return;
		//}

		EntityLocation loc1 = BaseThemeGenerator.pickCommonLocation(random, system1, 100f, false, null);
		//if (loc1 == null) {
		//	doDataFail();
		//	return;
		//}

		EntityLocation loc2 = BaseThemeGenerator.pickHiddenLocation(random, system2, 100f, null);
		//if (loc2 == null) {
		//	doDataFail();
		//	return;
		//}

		EntityLocation loc3 = BaseThemeGenerator.pickCommonLocation(random, system2, 100f, true,null);
		//if (loc3 == null) {
		//	doDataFail();
		//	return;
		//}

		AddedEntity minion = BaseThemeGenerator.addNonSalvageEntity(system1, loc1, "omicron_minion1", "independent");
		Global.getSector().getMemoryWithoutUpdate().set(PROMISE_MINION_KEY, minion);

		AddedEntity omicron = BaseThemeGenerator.addNonSalvageEntity(system2, loc2, "omicron_object", "independent");
		Global.getSector().getMemoryWithoutUpdate().set(KASSADAR_PLANET_KEY, omicron);

		AddedEntity station = BaseThemeGenerator.addEntity(system2, loc3, "promise_station", Factions.NEUTRAL);

		addDerelict(system2, omicron.entity, "tremor_attack", ShipRecoverySpecial.ShipCondition.BATTERED, 200f, false);
		addDerelict(system2, omicron.entity, "kindling_lancer", ShipRecoverySpecial.ShipCondition.BATTERED, 270f, false);
		addDerelict(system2, omicron.entity, "hammerhead_Balanced", ShipRecoverySpecial.ShipCondition.AVERAGE, 300f, false);
		addDerelict(system2, omicron.entity, "kite_Standard", ShipRecoverySpecial.ShipCondition.AVERAGE, 350f, true);
		addDerelict(system2, omicron.entity, "lustrare_attack", ShipRecoverySpecial.ShipCondition.BATTERED, 375f, false);
		addDerelict(system2, omicron.entity, "brilliant_Standard", ShipRecoverySpecial.ShipCondition.BATTERED, 400f, false);
		addDerelict(system2, omicron.entity, "scintilla_Strike", ShipRecoverySpecial.ShipCondition.BATTERED, 450f, false);

		DebrisFieldTerrainPlugin.DebrisFieldParams params1 = new DebrisFieldTerrainPlugin.DebrisFieldParams(
				360f, // field radius - should not go above 1000 for performance reasons
				1.2f, // density, visual - affects number of debris pieces
				10000000f, // duration in days
				10000000f); // days the field will keep generating glowing pieces
		params1.source = DebrisFieldTerrainPlugin.DebrisFieldSource.MIXED;
		params1.baseSalvageXP = 750; // base XP for scavenging in field
		SectorEntityToken debrisInner1 = Misc.addDebrisField(system2, params1, StarSystemGenerator.random);
		debrisInner1.setSensorProfile(800f);
		debrisInner1.setDiscoverable(true);
		debrisInner1.setCircularOrbit(omicron.entity, 360, 0, 180f);

		//if (minion == null || minion.entity == null) {
		//	doDataFail();
		//	return;
		//}

		//if (omicron == null || omicron.entity == null) {
		//	doDataFail();
		//	return;
		//}

		Global.getSector().getMemoryWithoutUpdate().set(PROMISE_SYSTEM1_KEY, system1);
		Global.getSector().getMemoryWithoutUpdate().set(PROMISE_SYSTEM2_KEY, system2);

		PlanetAPI star = system2.getStar();

		system2.setName("Promise");
		star.setName("Promise");
	}

	//Adds the relevant intel to the main intel manager
	protected void addIntel() {
		TextPanelAPI text = dialog.getTextPanel();
		AddedEntity minion = getOmicronMinion();

		tahlan_HuntForKassadarIntel intel = new tahlan_HuntForKassadarIntel(minion, this);
		Global.getSector().getIntelManager().addIntel(intel, false, text);
	}

	//Sets up the NPC shown during the bar event
	@Override
	protected void regen(MarketAPI market) {
		if (this.market == market) return;
		super.regen(market);

		if (person.getGender() == Gender.MALE) {
			person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "promise_officer"));
		} else {
			person.setPortraitSprite(Global.getSettings().getSpriteName("intel", "old_spacer_female"));
		}
	}

	//The text shown when you enter the bar screen
	@Override
	public void addPromptAndOption(InteractionDialogAPI dialog) {
		super.addPromptAndOption(dialog);
		
		regen(dialog.getInteractionTarget().getMarket());
		
		TextPanelAPI text = dialog.getTextPanel();
		text.addPara("A few shabby-looking ship captains share drinks, telling stories and occasionally " +
				"bursting out in laughter and drawing the eyes of other spacers.");

		Color c = Misc.getHighlightColor();
		c = Misc.getStoryOptionColor();
		
		dialog.getOptionPanel().addOption("Join the patrol officers in their discussion", this,
				c, null);
	}
	
	
	@Override
	public void init(InteractionDialogAPI dialog) {
		super.init(dialog);
		
		done = false;
		
		dialog.getVisualPanel().showPersonInfo(person, true);
		
		optionSelected(null, OptionId.INIT);
	}

	//The dialogue itself
	@Override
	public void optionSelected(String optionText, Object optionData) {
		if (!(optionData instanceof OptionId)) {
			return;
		}
		OptionId option = (OptionId) optionData;
		
		OptionPanelAPI options = dialog.getOptionPanel();
		TextPanelAPI text = dialog.getTextPanel();
		options.clearOptions();
		
		switch (option) {
		case INIT:
			text.addPara("One of the ship commanders brags at length about \"That one time I phased through two " +
					"Sunders just to hit a Colossus in the engines with a Reaper, you should've seen it! Took out " +
					"their command and scattered the whole fleet.\" Another recounts narrowly dodging a Luddic Path " +
					"raider's torpedo only for it to slam into another Pather's frigate and vaporize it.");

			text.addPara("One wearing a faded Hegemony medal recounts a time where she fought tooth-and-nail " +
					"to keep flankers off the tail of an Onslaught-class while it tore through a horde of \"those " +
					"damn things\".");
			options.addOption("Tell your own story", OptionId.TELL_STORY);
			options.addOption("Get up and leave them to their boasting", OptionId.LEAVE);
			break;
		case TELL_STORY:
			text.addPara("You tell a tale of one of your own encounters with death, having fought through " +
					"uncertain odds and coming out victorious. You're unsure if it's your story-telling prowess " +
					"or just the amount of drink that's vanishing into these captains. Your final point - pinging " +
					"green to let the rest of the fleet know you've won - stirs up cheers from around the table. " +
					"While they chatter, you have a chance to act with the spotlight on you.");
			options.addOption("Ask if they've heard anything interesting recently", OptionId.CONTINUE_1);
			options.addOption("Make a dramatic exit", OptionId.LEAVE);
			break;
		case CONTINUE_1:
			text.addPara("A young officer jumps at the opportunity. \"Heard about this weird ship from some " +
					"militia types when I was out drinking at Eochu Bres. Came outta nowhere, lightning fast. No " +
					"transponder, so they caught up to tell 'em before the local patrols caught 'em and got him " +
					"in any real trouble. Next thing you know, fast picket intercepts and goes in guns blazing. " +
					"Wiped out that frigate just like that. Didn't want to get involved, so they burned out.\"");
			options.addOption("\"Did they say where exactly the ship was?\"", OptionId.CONTINUE_2);
			generate();
			break;
		case CONTINUE_2:
			text.addPara("The captain frowns. \"Damn, can't remember. If you poked around there, you might " +
					"be able to find it. Only got told about it a couple days ago - it's probably still there.\"");
			BarEventManager.getInstance().notifyWasInteractedWith(this);
			addIntel();
			options.addOption("\"Thanks, anyway.\"", OptionId.CONTINUE_3);
			break;
		case CONTINUE_3:
			text.addPara("You listen a little more as a rugged and retired commander from Asharu begins " +
					"ranting about how some people over-rely on \"fancy-schmancy laser beams and teleporting " +
					"blue ships\", eventually leading into an argument about the dependency of low-tech " +
					"classics versus the endless potential of high-tech state-of-the-art tech. ");
			options.addOption("Leave during the commotion", OptionId.LEAVE);
			break;
		case LEAVE:
			noContinue = true;
			done = true;
			break;
		}
	}

	//This handles if the event, for some reason, failed to activate as intended.
	protected transient boolean failed = false;
	protected void doDataFail() {
		failed = true;
	}

	protected void addDerelict (StarSystemAPI system, SectorEntityToken focus, String variantId,
								ShipRecoverySpecial.ShipCondition condition, float orbitRadius, boolean recoverable){
		DerelictShipEntityPlugin.DerelictShipData params = new DerelictShipEntityPlugin.DerelictShipData(new ShipRecoverySpecial.PerShipData(variantId, condition), false);
		SectorEntityToken ship = BaseThemeGenerator.addSalvageEntity(system, Entities.WRECK, Factions.NEUTRAL, params);
		ship.setDiscoverable(true);

		float orbitDays = orbitRadius / (10f + (float) Math.random() * 5f);
		ship.setCircularOrbit(focus, (float) Math.random() * 360f, orbitRadius, orbitDays);

		if (recoverable) {
			SalvageSpecialAssigner.ShipRecoverySpecialCreator creator = new SalvageSpecialAssigner.ShipRecoverySpecialCreator(null, 0, 0, false, null, null);
			Misc.setSalvageSpecial(ship, creator.createSpecial(ship, null));
		}

	}

	//All of these are settings for the person you interact with
	@Override
	protected String getPersonFaction() {
		return Factions.INDEPENDENT;
	}
	@Override
	protected String getPersonRank() {
		return Ranks.AGENT;
	}
	@Override
	protected String getPersonPost() {
		return Ranks.POST_AGENT;
	}
	@Override
	protected String getPersonPortrait() {
		return null;
	}
	@Override
	protected Gender getPersonGender() {
		return Gender.MALE;
	}
	
}


