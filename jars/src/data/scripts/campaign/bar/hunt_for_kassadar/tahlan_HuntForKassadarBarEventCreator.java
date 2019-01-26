//Adapted from Inventor Raccoon's bar event creator by Nicke535
//	Adds the Hunt for Kassadar into the var event creators
package data.scripts.campaign.bar.hunt_for_kassadar;

import com.fs.starfarer.api.impl.campaign.intel.bar.PortsideBarEvent;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator;

public class tahlan_HuntForKassadarBarEventCreator extends BaseBarEventCreator {

	//Creates our bar event
	public PortsideBarEvent createBarEvent() {
		return new tahlan_HuntForKassadarBarEvent();
	}

	@Override
	public float getBarEventAcceptedTimeoutDuration() {
		return 10000000000f; // Alex took the easy way out, didn't he? well, we might as well do the same
	}

	//We don't need a custom frequency weight
	@Override
	public float getBarEventFrequencyWeight() {
			return super.getBarEventFrequencyWeight();
	}
}
