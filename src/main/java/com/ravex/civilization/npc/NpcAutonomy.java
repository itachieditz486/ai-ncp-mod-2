package com.ravex.civilization.npc;

import com.ravex.civilization.RavexConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility-style activity chooser for an idle NPC. Runs once a second on the server and only when no mission, target or
 * recent player command is active. Role, equipment, inventory, health, time of day and nearby resources decide what the
 * NPC starts (each start is a real mission using the normal goals). Random initial delay + per-role weights keep NPCs
 * from doing the same thing at the same time. Autonomous NPCs work silently (no chat spam).
 */
public final class NpcAutonomy {
    private final NpcEntity npc;
    private int cooldown;
    private int eatCooldown;
    private int shareCooldown;

    public NpcAutonomy(NpcEntity npc) {
        this.npc = npc;
        this.cooldown = 200 + npc.getRandom().nextInt(1000);
        this.shareCooldown = 300 + npc.getRandom().nextInt(600);
    }

    /** Called every 20 ticks. */
    public void tick(ServerLevel level) {
        eatIfNeeded();
        if (!RavexConfig.AUTONOMY_ENABLED.get()) return;
        if (npc.mission() != NpcMission.IDLE || npc.isThinking() || npc.isGuiHeld()) return;
        if (npc.getTarget() != null) return;
        cooperate(level);
        if (npc.tickCount - npc.lastCommandTick() < 600) return;   // just after a player order: don't wander off
        if ((shareCooldown -= 20) <= 0) {
            shareCooldown = 400 + npc.getRandom().nextInt(400);
            share(level);
        }
        if ((cooldown -= 20) > 0) return;
        decide(level);
    }

    // ---- survival ---------------------------------------------------------------------------------------------------
    /** Hunger is not simulated; low health makes the NPC really eat a food item from its inventory to heal. */
    private void eatIfNeeded() {
        if (eatCooldown > 0) { eatCooldown--; return; }
        if (npc.getHealth() >= npc.getMaxHealth() - 5.0F || npc.getTarget() != null) return;
        for (int i = 0; i < npc.inventory.getContainerSize(); i++) {
            ItemStack s = npc.inventory.getItem(i);
            FoodProperties f = s.get(DataComponents.FOOD);
            if (f == null || !f.effects().isEmpty()) continue;   // skip rotten flesh, suspicious stew etc.
            s.shrink(1);
            if (s.isEmpty()) npc.inventory.setItem(i, ItemStack.EMPTY);
            npc.heal(Math.max(2.0F, f.nutrition()));
            npc.level().playSound(null, npc.getX(), npc.getY(), npc.getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.6F, 1.0F);
            eatCooldown = 3;
            return;
        }
    }

    // ---- cooperation ------------------------------------------------------------------------------------------------
    /** Join a nearby friendly NPC's fight against a hostile mob. */
    private void cooperate(ServerLevel level) {
        if (npc.getHealth() < 10.0F) return;
        for (NpcEntity other : level.getEntitiesOfClass(NpcEntity.class, npc.getBoundingBox().inflate(16.0),
                n -> n != npc && n.isAlive())) {
            LivingEntity t = other.getTarget();
            if (t != null && t.isAlive() && t instanceof Enemy) {
                npc.setTarget(t);
                return;
            }
        }
    }

    /** Hand spare items to a nearby friendly NPC that lacks them (real inventory-to-inventory transfer). */
    private void share(ServerLevel level) {
        for (NpcEntity other : level.getEntitiesOfClass(NpcEntity.class, npc.getBoundingBox().inflate(6.0),
                n -> n != npc && n.isAlive() && !npc.isDesignatedHostile(n.getUUID()))) {
            // food
            if (other.foodCount() < 2 && npc.foodCount() >= 6) {
                if (transfer(other, s -> s.has(DataComponents.FOOD) && s.get(DataComponents.FOOD).effects().isEmpty(), 2)) return;
            }
            // pickaxe
            if (!other.hasPickaxe() && npc.pickaxeCount() >= 2) {
                if (transfer(other, s -> s.getItem() instanceof PickaxeItem, 1)) return;
            }
            // armor piece for an empty slot
            for (int i = 0; i < npc.inventory.getContainerSize(); i++) {
                ItemStack s = npc.inventory.getItem(i);
                if (s.getItem() instanceof ArmorItem a) {
                    EquipmentSlot slot = a.getType().getSlot();
                    if (other.getItemBySlot(slot).isEmpty() && !other.inventoryHasArmorFor(slot)) {
                        if (transfer(other, x -> x == s, 1)) return;
                    }
                }
            }
        }
    }

    private boolean transfer(NpcEntity to, java.util.function.Predicate<ItemStack> what, int count) {
        for (int i = 0; i < npc.inventory.getContainerSize(); i++) {
            ItemStack s = npc.inventory.getItem(i);
            if (s.isEmpty() || !what.test(s)) continue;
            ItemStack out = npc.inventory.removeItem(i, Math.min(count, s.getCount()));
            ItemStack left = to.inventory.addItem(out);
            if (!left.isEmpty()) npc.inventory.addItem(left);   // receiver was full: take it back, nothing lost
            return left.getCount() < out.getCount();
        }
        return false;
    }

    // ---- decisions --------------------------------------------------------------------------------------------------
    private static boolean isNight(ServerLevel level) {
        long t = level.getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    private void decide(ServerLevel level) {
        var r = npc.getRandom();
        cooldown = 100 + r.nextInt(300);

        // go home when it is night or the inventory is nearly full
        if (npc.home() != null && npc.distanceToSqr(Vec3.atCenterOf(npc.home())) > 64.0
                && (isNight(level) || npc.usedSlots() >= 22)) {
            npc.startAutonomous("home", 0);
            return;
        }
        if (isNight(level)) { cooldown = 400 + r.nextInt(400); return; }   // rest near home

        // (activity, weight) for this role
        record Opt(String act, int w) {}
        List<Opt> opts = new ArrayList<>();
        boolean foodLow = npc.foodCount() < 4 && animalsNearby(level);
        switch (npc.role()) {
            case "lumberjack" -> { opts.add(new Opt("wood", 6)); opts.add(new Opt("explore", 2)); if (foodLow) opts.add(new Opt("hunt", 2)); }
            case "miner" -> { opts.add(new Opt("coal", 3)); opts.add(new Opt("stone", 4)); opts.add(new Opt("wood", 1)); opts.add(new Opt("explore", 2)); }
            case "farmer", "cook" -> { if (foodLow) opts.add(new Opt("hunt", 6)); opts.add(new Opt("wood", 2)); opts.add(new Opt("explore", 3)); }
            case "guard", "warrior" -> { opts.add(new Opt("guard", 5)); opts.add(new Opt("explore", 2)); if (foodLow) opts.add(new Opt("hunt", 2)); }
            case "hunter" -> { opts.add(new Opt("hunt", foodLow ? 6 : 2)); opts.add(new Opt("explore", 3)); opts.add(new Opt("wood", 1)); }
            case "builder" -> { opts.add(new Opt("wood", 4)); opts.add(new Opt("stone", 3)); opts.add(new Opt("explore", 1)); }
            default -> { opts.add(new Opt("explore", 6)); opts.add(new Opt("wood", 2)); if (foodLow) opts.add(new Opt("hunt", 2)); }
        }
        opts.add(new Opt("rest", 2));
        int total = 0;
        for (Opt o : opts) total += o.w();
        int roll = r.nextInt(total);
        String pick = "rest";
        for (Opt o : opts) {
            if ((roll -= o.w()) < 0) { pick = o.act(); break; }
        }
        int count = switch (pick) {
            case "wood" -> 6 + r.nextInt(7);
            case "stone" -> 6 + r.nextInt(5);
            case "coal" -> 4 + r.nextInt(5);
            default -> 0;
        };
        if (pick.equals("rest")) { cooldown = 300 + r.nextInt(600); return; }
        if (!npc.startAutonomous(pick, count)) npc.startAutonomous("explore", 0);   // e.g. no pickaxe / no home: explore instead
    }

    private boolean animalsNearby(ServerLevel level) {
        AABB box = npc.getBoundingBox().inflate(24.0);
        return !level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && (e instanceof Cow || e instanceof Pig || e instanceof Sheep || e instanceof Chicken)).isEmpty();
    }
}
