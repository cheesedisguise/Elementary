package dev.elementary.data;

import dev.elementary.element.Element;

public class PlayerData {
    public Element element;
    public int tier = 1;
    public double challengeProgress = 0;
    /** Highest milestone already announced (25/50/75). */
    public int challengeMilestone = 0;
    public boolean abilityMessages = true;

    public PlayerData(Element element) {
        this.element = element;
    }
}
