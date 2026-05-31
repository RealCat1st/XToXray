/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.renderer.culling.Frustum
 *  net.minecraft.client.renderer.debug.DebugRenderer
 *  net.minecraft.gizmos.GizmoStyle
 *  net.minecraft.gizmos.Gizmos
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.monster.Enemy
 *  net.minecraft.world.phys.AABB
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.xtoxray.client.mixin;

import com.xtoxray.XrayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={DebugRenderer.class})
public class MixinDebugRenderer {
    @Inject(method={"emitGizmos(Lnet/minecraft/client/renderer/culling/Frustum;DDDF)V"}, at={@At(value="RETURN")})
    private void onEmitGizmos(Frustum frustum, double camX, double camY, double camZ, float tickDelta, CallbackInfo ci) {
        if (!XrayState.getInstance().isShowHitboxes() || !XrayState.getInstance().isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        for (Entity entity : mc.level.entitiesForRendering()) {
            Mob mob;
            if (!(entity instanceof Enemy) && (!(entity instanceof Mob) || !(mob = (Mob)entity).isAggressive())) continue;
            Gizmos.cuboid((AABB)entity.getBoundingBox(), (GizmoStyle)GizmoStyle.stroke((int)-65536));
        }
    }
}



