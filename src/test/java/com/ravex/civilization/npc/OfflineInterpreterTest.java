package com.ravex.civilization.npc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Command parsing must work with no AI and no internet. Pure logic: no Minecraft classes are touched. */
class OfflineInterpreterTest {
    private static void check(String text, String action, int count, String arg) {
        OfflineInterpreter.Result r = OfflineInterpreter.interpret(text, "en");
        assertEquals(action, r.action(), "action for: " + text);
        assertEquals(count, r.count(), "count for: " + text);
        assertEquals(arg, r.arg(), "arg for: " + text);
    }

    @Test void wood() {
        check("Yassine, collect 32 oak logs.", "collect_wood", 32, "oak");
        check("Collect 32 wood", "collect_wood", 32, "");
        check("Collect 16 spruce logs", "collect_wood", 16, "spruce");
        check("Collect 20 birch logs", "collect_wood", 20, "birch");
        check("collect wood", "collect_wood", 8, "");
        check("Bring me 32 wood", "collect_wood", 32, "");
        check("collect 2 stacks of oak logs", "collect_wood", 128, "oak");
        check("collect 10 dark oak logs", "collect_wood", 10, "dark_oak");
    }

    @Test void mining() {
        check("Collect 16 iron", "collect", 16, "iron");
        check("Collect 10 diamonds", "collect", 10, "diamond");
        check("Collect 32 iron ore", "collect", 32, "iron");
        check("Collect ores", "collect", 0, "ores");
        check("Go caving", "collect", 0, "caving");
        check("Go mining", "collect", 0, "ores");
        check("Collect stone", "collect", 0, "stone");
    }

    @Test void attacks() {
        check("Attack.", "attack_mobs", 0, "");
        check("Attack zombies", "attack_mobs", 0, "zombie");
        check("Attack skeletons", "attack_mobs", 0, "skeleton");
        check("Attack creepers", "attack_mobs", 0, "creeper");
        check("Attack spiders", "attack_mobs", 0, "spider");
        check("Attack endermen", "attack_mobs", 0, "enderman");
        check("Attack the nearest zombie", "attack_mobs", 1, "zombie");
        check("Attack that mob", "attack_mobs", 1, "look");
        check("Kill nearby hostile mobs", "attack_mobs", 0, "");
        check("Stop attacking", "stop_attack", 0, "");
    }

    @Test void movementAndDefence() {
        check("Follow me", "follow", 0, "");
        check("Stop following me", "stop", 0, "");
        check("Stay here", "stay", 0, "");
        check("Come here", "come", 0, "");
        check("Return to me", "come", 0, "");
        check("Protect me", "protect", 0, "");
        check("Defend me from enemies", "protect", 0, "");
        check("Guard this location", "guard", 0, "");
        check("Protect this area", "guard", 0, "");
        check("Join my team", "join_team", 0, "");
        check("Leave my team", "leave_team", 0, "");
    }

    @Test void homeAndDelivery() {
        check("Return home", "return_home", 0, "");
        check("Return home with collected resources", "return_home", 0, "");
        check("Come back and give me the resources", "deliver", 0, "");
        check("give me the wood", "deliver", 0, "");
    }

    @Test void foodHunting() {
        check("get food", "hunt", 0, "");
        check("hunt animals", "hunt", 0, "");
    }

    @Test void french() {
        check("récolte 32 bûches de chêne", "collect_wood", 32, "oak");
        check("récolte 16 fer", "collect", 16, "iron");
        check("attaque les zombies", "attack_mobs", 0, "zombie");
        check("attaque le zombie le plus proche", "attack_mobs", 1, "zombie");
        check("va explorer les grottes", "collect", 0, "caving");
        check("donne-moi les ressources", "deliver", 0, "");
        check("rentre à la maison", "return_home", 0, "");
        check("reste ici", "stay", 0, "");
    }

    @Test void arabic() {
        check("اجمع 32 خشب بلوط", "collect_wood", 32, "oak");
        check("اجمع 10 ألماس", "collect", 10, "diamond");
        check("هاجم الزومبي", "attack_mobs", 0, "zombie");
        check("استكشف الكهوف", "collect", 0, "caving");
        check("أعطني الموارد", "deliver", 0, "");
        check("احمني", "protect", 0, "");
        check("تعال", "come", 0, "");
        check("ارجع للمنزل", "return_home", 0, "");
    }

    @Test void unsupportedIsHonest() {
        assertEquals("none", OfflineInterpreter.interpret("build me a house", "en").action());
    }
}
