package serverClasses;

import tage.ai.behaviortrees.BTCondition;

public class OneSecPassed extends BTCondition{
    NPCController npcC;
    NPC npc;

    public OneSecPassed(NPCController npcCtrl, NPC n, boolean tn){
        super(tn);
        npcC = npcCtrl;
        npc = n;
    }

    protected boolean check(){
        return npcC.checkOneSecPassed();
    }
}