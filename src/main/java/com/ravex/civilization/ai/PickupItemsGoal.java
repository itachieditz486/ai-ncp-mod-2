package com.ravex.civilization.ai;

import com.ravex.civilization.RavexConfig;
import com.ravex.civilization.npc.NpcEntity;
import com.ravex.civilization.npc.NpcMission;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Detects real dropped ItemEntity objects, walks to reachable ones that fit in the inventory and picks them up
 * (stack moved into the NPC's inventory, never copied). Equipment and food are preferred over other items.
 * Only runs when the NPC is not fighting and is idle / following / protecting / hunting.
 */
public class PickupItemsGoal extends Goal {
    private final NpcEntity npc;
    private ItemEntity item;
    private int scanDelay;
    private int repath;
    private int ticks;
    private final Set<Integer> unreachable = new HashSet<>();

    public PickupItemsGoal(NpcEntity npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean missionAllows() {
        NpcMission m = npc.mission();
        return m == NpcMission.IDLE || m == NpcMission.FOLLOW || m == NpcMission.PROTECT || m == NpcMission.HUNT
                || m == NpcMission.FOLLOW_NPC;
    }

    @Override
    public boolean canUse() {
        if (!missionAllows() || npc.getTarget() != null || npc.isGuiHeld()) return false;
        if (scanDelay-- > 0) return false;
        scanDelay = 10;
        if (!(npc.level() instanceof ServerLevel level)) return false;
        double r = RavexConfig.PICKUP_RADIUS.get();
        List<ItemEntity> list = new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, npc.getBoundingBox().inflate(r, 4.0, r),
                e -> npc.canPickUp(e) && !unreachable.contains(e.getId())));
        if (list.isEmpty()) return false;
        list.sort(Comparator.comparingDouble((ItemEntity e) -> (npc.isUsefulItem(e.getItem()) ? 0 : 10000) + npc.distanceToSqr(e)));
        PathNavigation nav = npc.getNavigation();
        int tried = 0;
        for (ItemEntity e : list) {
            if (tried++ >= 3) break;
            Path p = nav.createPath(e.blockPosition(), 1);
            if (p != null && p.canReach()) {
                item = e;
                return true;
            }
            unreachable.add(e.getId());
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return item != null && missionAllows() && npc.getTarget() == null && !npc.isGuiHeld() && ticks < 400 && npc.canPickUp(item);
    }

    @Override
    public void start() {
        ticks = 0;
        repath = 0;
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
        item = null;
        if (unreachable.size() > 64) unreachable.clear();
    }

    @Override
    public void tick() {
        ticks++;
        if (npc.distanceToSqr(item) <= 2.25) {
            npc.pickUp(item);
            item = null;
            return;
        }
        if (--repath <= 0) {
            repath = 10;
            npc.getNavigation().moveTo(item, 1.1);
        }
    }
}
