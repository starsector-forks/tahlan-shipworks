//By Nicke535
//A submarket that allows you to amalgamate different hulls into special ships depending on the combination
//  We're Magicka-ing it up, here!
package data.scripts.campaign.submarkets;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.util.Highlights;
import com.fs.starfarer.api.util.Misc;

import java.util.*;

public class tahlan_GreatHousesConversionSubmarket extends BaseSubmarketPlugin {

    //This is how much reputation is needed to be allowed into the Great Houses submarket.
    private static final RepLevel REPUTATION_NEEDED_FOR_MARKET = RepLevel.WELCOMING;

    //The ID of the Great Houses faction
    private static final String GREAT_HOUSES_FACTION_ID = "tahlan_greathouses";

    //A list of all valid refits and their data. The refits at the top of the list take precedence if multiple
    //alternatives could be "crafted", but overlap should still be avoided
    private static final List<RefitData> REFIT_DATAS = new ArrayList<>();
    static {
        REFIT_DATAS.add(new RefitData("tahlan_legion_gh", "legion", "doom"));
        REFIT_DATAS.add(new RefitData("tahlan_Castigator_knight", "eagle", "onslaught"));
    }


    //Initialize some in-script variables used here and there
    private int lastAppliedMonth = 0;
    private List<FleetMemberAPI> leftoversLastMonth = new ArrayList<>();
    private List<FleetMemberAPI> excludedFromTrimming = new ArrayList<>();

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
    public String getSellVerb() { return "Deposit"; }
    @Override
    public String getBuyVerb() { return "Retrieve"; }


    //The main advance() function: mostly just detects if a month has passed, and calls our monthly application if so
    @Override
    public void advance(float amount) {
        //Run our overridden advance function
        super.advance(amount);

        //Only tick on the second day each month
        if (Global.getSector().getClock().getDay() != 2) {
            return;
        }

        //Checks our clock to see if our month is different from last time we ticked; if it is, we run a tick
        if (Global.getSector().getClock().getMonth() > lastAppliedMonth) {
            lastAppliedMonth = Global.getSector().getClock().getMonth();
            monthlyApplication();
        }
    }


    //This function runs once a month, and handles the amalgamation process
    private void monthlyApplication() {
        //Go through all our refit data, and see which ones match. Keep going until we've tried them all and failed them
        List<String> completedRefits = new ArrayList<>();
        boolean shouldBreakFull = false;
        while (!shouldBreakFull) {
            shouldBreakFull = true;
            for (RefitData data : REFIT_DATAS) {
                //Stores a varible for each component hull type, so we know how many we need of each
                Map<String, Integer> requiredHulls = new HashMap<>();
                for (int i = 0; i < data.componentHulls.length; i++) {
                    if (requiredHulls.containsKey(data.componentHulls[i])) {
                        requiredHulls.put(data.componentHulls[i], requiredHulls.get(data.componentHulls[i])+1);
                    } else {
                        requiredHulls.put(data.componentHulls[i], 1);
                    }
                }

                //Then, go through the ships in our cargo to see if we have enough to actually create the refit
                List<FleetMemberAPI> membersToRemove = new ArrayList<>();
                for (FleetMemberAPI member : getCargo().getMothballedShips().getMembersInPriorityOrder()) {
                    //Ignore any ships that aren't required
                    if (requiredHulls.containsKey(member.getHullSpec().getBaseHullId())) {
                        //This hull was required; tick down how many hulls we need
                        requiredHulls.put(member.getHullSpec().getBaseHullId(), requiredHulls.get(member.getHullSpec().getBaseHullId())-1);

                        //If that was the last hull we needed, remove the requirement
                        if (requiredHulls.get(member.getHullSpec().getBaseHullId()) <= 0) {
                            requiredHulls.remove(member.getHullSpec().getBaseHullId());
                        }

                        //Finally, register that this hull is to be removed upon amalgamation
                        membersToRemove.add(member);
                    }
                }

                //If all our requirements were fulfilled, remove the appropriate members, register the refit as complete
                //Also break the loop so we have to start over from the beginning again
                if (requiredHulls.isEmpty()) {
                    for (FleetMemberAPI member : membersToRemove) {
                        getCargo().getMothballedShips().removeFleetMember(member);
                    }
                    completedRefits.add(data.destHull);
                    shouldBreakFull = false;
                    break;
                }
            }
        }

        //Go through all ships left over, except the ones that are excluded from "trimming". Remove all the ones that were also leftover last month
        List<FleetMemberAPI> trimList = new ArrayList<>();
        for (FleetMemberAPI member : getCargo().getMothballedShips().getMembersInPriorityOrder()) {
            if (excludedFromTrimming.contains(member)) {
                continue;
            }

            //Check if this member is was already marked for trimming last month. If so, we're gonna delete them later
            if (leftoversLastMonth.contains(member)) {
                trimList.add(member);
                leftoversLastMonth.remove(member);
            }

            //Otherwise, we add them to the leftovers
            else {
                leftoversLastMonth.add(member);
            }
        }
        for (FleetMemberAPI member : trimList) {
            getCargo().getMothballedShips().removeFleetMember(member);
        }

        //Now that we're done with finding all the ships we completed refit-wise, we show a message to the player that their refits are done...
        if (!completedRefits.isEmpty()) {
            Global.getSector().getCampaignUI().addMessage(
                    market.getName() + ": One or more Great Houses Refits have been successfully completed.",
                    Global.getSettings().getColor("standardTextColor"),
                    market.getName(), "Great Houses Refits",
                    market.getFaction().getBaseUIColor(),
                    Global.getSettings().getColor("yellowTextColor"));
        }

        //...and add all the ships to the submarket
        for (String hullID : completedRefits) {
            FleetMemberAPI newMember = getCargo().getMothballedShips().addFleetMember(hullID + "_Hull");
            excludedFromTrimming.add(newMember);
        }
    }


    //The text that is returned when you are informed *why* you aren't allowed to sell stuff on the normal market
    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_SELL) {
            return "This submarket does not conduct commodity trade.";
        }

        return "AN ERROR OCURRED IN tahlan_GreatHousesConversionSubmarket.java: CONTACT Nicke535 OR Nia";
    }

    //Description text for trying to sell a ship to the submarket that you, for some reason, can't.
    //This should never appear, so always show error text
    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return "AN ERROR OCURRED IN tahlan_GreatHousesConversionSubmarket.java: CONTACT Nicke535 OR Nia";
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
}