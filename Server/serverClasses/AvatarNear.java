package serverClasses;

import tage.ai.behaviortrees.BTCondition;

public class AvatarNear extends BTCondition{
    NPC npc;
    NPCController npcc;
    GameAIServerUDP gameServer;

    public AvatarNear(GameAIServerUDP s, NPCController nc, NPC n, boolean toNegate){

        super(toNegate);
        npc = n;
        npcc = nc;
        gameServer = s;

    }

    protected boolean check(){
        gameServer.sendCheckForAvatarNear();
        return npcc.getNearFlag();
    }
}