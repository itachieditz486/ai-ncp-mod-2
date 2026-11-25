package com.ravex.civilization.ai;

import com.ravex.civilization.npc.NpcEntity;
import com.ravex.civilization.npc.NpcMission;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

/** Walks to a random reachable spot 12-28 blocks from home (or from where it stands) over normal terrain. */
public class ExploreGoal extends Goal {
    private final NpcEntity npc;
    private BlockPos dest;
    private int ticks;

    public ExploreGoal(NpcEntity npc) {
        this.npc = npc;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return npc.mission() == NpcMission.EXPLORE && npc.getTarget() == null && !npc.isGuiHeld();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        ticks = 0;
        dest = pickDestination();
        if (dest == null) {
            npc.finishMission("");
            return;
        }
        npc.getNavigation().moveTo(dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, 0.9);
    }

    private BlockPos pickDestination() {
        if (!(npc.level() instanceof ServerLevel level)) return null;
        BlockPos base = npc.home() != null ? npc.home() : npc.blockPosition();
        PathNavigation nav = npc.getNavigation();
        for (int attempt = 0; attempt < 6; attempt++) {
            double ang = npc.getRandom().nextDouble() * Math.PI * 2;
            int d = 12 + npc.getRandom().nextInt(17);
            BlockPos col = new BlockPos(base.getX() + (int) (Math.cos(ang) * d), 0, base.getZ() + (int) (Math.sin(ang) * d));
            if (!level.isLoaded(col)) continue;
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, col);
            Path p = nav.createPath(top, 1);
            if (p != null && p.canReach()) return top;
        }
        return null;
    }

    @Override
    public void tick() {
        if (++ticks > 700 || npc.getNavigation().isDone()) {
            npc.finishMission(""); // autonomous: silent; mission returns to IDLE
        }
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }
}
