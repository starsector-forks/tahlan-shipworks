//By Nicke535
//A submarket that allows you to amalgamate different hulls into special ships depending on the combination
//  We're Magicka-ing it up, here!
package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.EconomyAPI.EconomyUpdateListener;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import java.util.*;

public class tahlan_GreatHousesConversionSubmarketUNFINISHED extends BaseSubmarketPlugin {

    //This is how much reputation is needed to be allowed into the Great Houses submarket.
    private static final RepLevel REPUTATION_NEEDED_FOR_MARKET = RepLevel.WELCOMING;

    //The ID of the Great Houses faction
    private static final String GREAT_HOUSES_FACTION_ID = "tahlan_great_houses";

    //A list of all valid refits and their data. The refits at the top of the list take precedence if multiple
    //alternatives could be "crafted", but overlap should still be avoided
    private static final List<RefitData> REFIT_DATAS = new ArrayList<>();
    static {
        REFIT_DATAS.add(new RefitData("tahlan_legion_gh", "legion", "doom"));
        REFIT_DATAS.add(new RefitData("tahlan_Castigator_knight", "eagle", "onslaught"));
    }


    //Initialize some in-script variables used here and there
    private MarketMonthEndReporter updateListener = null;

    //Initializer function; just run the original init
    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    //This submarket is ignored in the economy
    @Override
    public boolean isParticipatesInEconomy() {
        return false;
    }

    //Simply returns the tariff of the submarket
    @Override
    public float getTariff() {
        return market.getTariff().getModifiedValue();
    }

    //No commodities may be sold on the market
    @Override
    public boolean isIllegalOnSubmarket(String commodityId, TransferAction action) {
        return true;
    }

    //Prevent selling actions; really, this is the same as above, but required anyhow for some reason
    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        return action == TransferAction.PLAYER_SELL;
    }

    //Changes the verb for selling/bying ships (go figure!)
    @Override
    public String getSellVerb() {
        return "Deposit";
    }
    @Override
    public String getBuyVerb() {
        return "Retrieve";
    }


    //The main advance() function: mostly just adds a listener to the economy
    @Override
    public void advance(float amount) {
        //Run our overridden advance function
        super.advance(amount);

        //Get us an economy listener, if we don't already have one
        if (updateListener == null) {
            updateListener = new MarketMonthEndReporter(this);
            Global.getSector().getEconomy().addUpdateListener(updateListener);
        }
    }


    //This is the function that gets called by our economy update listener when the economy updates
    public void reportEconomyHasUpdated () {
        //Go through all our refit datas, and see which ones match. Keep going until we've tried them all and failed them
        boolean shouldBreakFull = false;
        while (!shouldBreakFull) {
            for (RefitData data : REFIT_DATAS) {
                //Stores a varible for each component hull type, so we know how many we need
                Map<String, Integer> requiredHulls = new HashMap<>();
                for (int i = 0; i < data.componentHulls.length; i++) {
                    if (requiredHulls.containsKey(data.componentHulls[i])) {
                        requiredHulls.put(data.componentHulls[i], requiredHulls.get(data.componentHulls[i])+1);
                    }
                }

                //CONTINUE HERE

                //First, find if the submarket has the ships needed for the refit
                for (FleetMemberAPI member : getCargo().getMothballedShips().getMembersListCopy()) {

                }
            }
            shouldBreakFull = true;
        }
    }


    //The text that is returned when you are informed *why* you aren't allowed to sell stuff on the normal market
    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) {
            return "This submarket does not conduct commodity trade.";
        }

        return "AN ERROR OCURRED IN tahlan_GreatHousesConversionSubmarketUNFINISHED.java: CONTACT Nicke535 OR Nia";
    }

    //Description text for trying to sell a ship to the submarket that you, for some reason, can't.
    //This should never appear, so always show error text
    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "AN ERROR OCURRED IN tahlan_GreatHousesConversionSubmarketUNFINISHED.java: CONTACT Nicke535 OR Nia";
    }

    //Checks if the market should be enabled; in our case, we just check if our reputation is good enough AND that the market is owned by the Sylphon
    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        RepLevel level = submarket.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        return level.isAtWorst(REPUTATION_NEEDED_FOR_MARKET) && this.market.getFactionId().equals(GREAT_HOUSES_FACTION_ID);
    }

    //An appendix to the tooltip: I honestly don't know exactly how this works, so trial-and-error it is
    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        //If the ui isn't enabled (we can't enter the market) there are two possibilities: the market isn't owned by the Great Houses, or we don't have enough standing. Tell the player that.
        if (!isEnabled(ui)) {
            if (!this.market.getFactionId().equals(GREAT_HOUSES_FACTION_ID)) {
                return "With the Great Houses driven off from this market, their specialized refit facilities are inoperable.";
            }
            return "Requires: " + submarket.getFaction().getDisplayName() + " - " +
                    REPUTATION_NEEDED_FOR_MARKET.getDisplayName().toLowerCase();
        }

        //Otherwise, we don't add any appendix (this may change later, for clarity)
        return null;
    }

    //Indicates which part of the appendix should be highlighted
    @Override
    public Highlights getTooltipAppendixHighlights(CoreUIAPI ui) {
        String appendix = getTooltipAppendix(ui);
        //If we don't have an appandix, we don't highlight any part of it (obviously!)
        if (appendix == null) {
            return null;
        }

        //Initialize the highlights
        Highlights h = new Highlights();

        //If we don't have the ui enabled (meaning we can't open the submarket) highlight our entire appendix in red
        if (!isEnabled(ui)) {
            h.setText(appendix);
            h.setColors(Misc.getNegativeHighlightColor());
        }

        //Returns our highlights
        return h;
    }

    //Checks if a ship is allowed to be sold to the submarket; this should match up with the related description earlier in the script
    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) {
            return false;
        }

        //If this wasn't a player selling something, the transaction is legal: we're just getting our stuff back!
        return false;
    }

    //The class for all the data related to a refit option
    static class RefitData {
        private final String destHull;
        private final String[] componentHulls;
        RefitData (String destHull, String ... componentHulls) {
            this.destHull = destHull;
            this.componentHulls = componentHulls;
        }
    }

    //The listener class we use to detect the global market updating
    class MarketMonthEndReporter implements EconomyUpdateListener {
        tahlan_GreatHousesConversionSubmarketUNFINISHED source;
        MarketMonthEndReporter (tahlan_GreatHousesConversionSubmarketUNFINISHED source) {
            this.source = source;
        }

        //When the economy updates, simply send back that data to the source script
        @Override
        public void economyUpdated() {
            source.reportEconomyHasUpdated();
        }

        //Do nothing here: we don't care about commodities
        @Override
        public void commodityUpdated(String commodityId) {}

        //The listener never expires
        @Override
        public boolean isEconomyListenerExpired() {
            return false;
        }
    }
}