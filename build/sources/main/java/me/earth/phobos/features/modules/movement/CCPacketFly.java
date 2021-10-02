package me.earth.phobos.features.modules.movement;

import me.earth.phobos.util.Timer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.util.math.BlockPos;
import me.earth.phobos.features.Feature;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import me.earth.phobos.event.events.PushEvent;
import me.earth.phobos.event.events.PacketEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import me.earth.phobos.util.EntityUtil;
import me.earth.phobos.event.events.UpdateWalkingPlayerEvent;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.util.internal.ConcurrentSet;
import java.util.Map;
import net.minecraft.network.play.client.CPacketPlayer;
import java.util.Set;
import me.earth.phobos.features.modules.Module;

public class CCPacketFly extends Module
{
    private final Set<CPacketPlayer> packets;
    private final Map<Integer, IDtime> teleportmap;
    private int flightCounter;
    private int teleportID;
    private static CCPacketFly instance;

    public CCPacketFly() {
        super("CCPacketFly", "EZ CC PHASE", Category.MOVEMENT, true, false, false);
        this.packets = (Set<CPacketPlayer>)new ConcurrentSet();
        this.teleportmap = new ConcurrentHashMap<Integer, IDtime>();
        this.flightCounter = 0;
        this.teleportID = 0;
        CCPacketFly.instance = this;
    }

    public static CCPacketFly getInstance() {
        if (CCPacketFly.instance == null) {
            CCPacketFly.instance = new CCPacketFly();
        }
        return CCPacketFly.instance;
    }

    @SubscribeEvent
    public void onUpdateWalkingPlayer(final UpdateWalkingPlayerEvent event) {
        if (event.getStage() == 1) {
            return;
        }
        CCPacketFly.mc.player.setVelocity(0.0, 0.0, 0.0);
        final boolean checkCollisionBoxes = this.checkHitBoxes();
        double speed = (CCPacketFly.mc.player.movementInput.jump && (checkCollisionBoxes || !EntityUtil.isMoving())) ? (checkCollisionBoxes ? 0.062 : (this.resetCounter(10) ? -0.032 : 0.062)) : (CCPacketFly.mc.player.movementInput.sneak ? -0.062 : (checkCollisionBoxes ? 0.0 : (this.resetCounter(4) ? -0.04 : 0.0)));
        if (checkCollisionBoxes && EntityUtil.isMoving() && speed != 0.0) {
            final double antiFactor = 2.5;
            speed /= antiFactor;
        }
        final boolean strafeFactor = true;
        final double[] strafing = this.getMotion((strafeFactor && checkCollisionBoxes) ? 0.031 : 0.26);
        for (int loops = 1, i = 1; i < loops + 1; ++i) {
            final double extraFactor = 1.0;
            CCPacketFly.mc.player.motionX = strafing[0] * i * extraFactor;
            CCPacketFly.mc.player.motionY = speed * i;
            CCPacketFly.mc.player.motionZ = strafing[1] * i * extraFactor;
            final boolean sendTeleport = true;
            this.sendPackets(CCPacketFly.mc.player.motionX, CCPacketFly.mc.player.motionY, CCPacketFly.mc.player.motionZ, sendTeleport);
        }
    }

    @SubscribeEvent
    public void onPacketSend(final PacketEvent.Send event) {
        if (event.getPacket() instanceof CPacketPlayer && !this.packets.remove(event.getPacket())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPushOutOfBlocks(final PushEvent event) {
        if (event.getStage() == 1) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPacketReceive(final PacketEvent.Receive event) {
        if (event.getPacket() instanceof SPacketPlayerPosLook && !Feature.fullNullCheck()) {
            final SPacketPlayerPosLook packet = event.getPacket();
            if (CCPacketFly.mc.player.isEntityAlive() && CCPacketFly.mc.world.isBlockLoaded(new BlockPos(CCPacketFly.mc.player.posX, CCPacketFly.mc.player.posY, CCPacketFly.mc.player.posZ), false) && !(CCPacketFly.mc.currentScreen instanceof GuiDownloadTerrain)) {
                this.teleportmap.remove(packet.getTeleportId());
            }
            this.teleportID = packet.getTeleportId();
        }
    }

    private boolean checkHitBoxes() {
        return !CCPacketFly.mc.world.getCollisionBoxes((Entity)CCPacketFly.mc.player, CCPacketFly.mc.player.getEntityBoundingBox().offset(-0.0, -0.1, -0.0)).isEmpty();
    }

    private boolean resetCounter(final int counter) {
        if (++this.flightCounter >= counter) {
            this.flightCounter = 0;
            return true;
        }
        return false;
    }

    private double[] getMotion(final double speed) {
        float moveForward = CCPacketFly.mc.player.movementInput.moveForward;
        float moveStrafe = CCPacketFly.mc.player.movementInput.moveStrafe;
        float rotationYaw = CCPacketFly.mc.player.prevRotationYaw + (CCPacketFly.mc.player.rotationYaw - CCPacketFly.mc.player.prevRotationYaw) * CCPacketFly.mc.getRenderPartialTicks();
        if (moveForward != 0.0f) {
            if (moveStrafe > 0.0f) {
                rotationYaw += ((moveForward > 0.0f) ? -45 : 45);
            }
            else if (moveStrafe < 0.0f) {
                rotationYaw += ((moveForward > 0.0f) ? 45 : -45);
            }
            moveStrafe = 0.0f;
            if (moveForward > 0.0f) {
                moveForward = 1.0f;
            }
            else if (moveForward < 0.0f) {
                moveForward = -1.0f;
            }
        }
        final double posX = moveForward * speed * -Math.sin(Math.toRadians(rotationYaw)) + moveStrafe * speed * Math.cos(Math.toRadians(rotationYaw));
        final double posZ = moveForward * speed * Math.cos(Math.toRadians(rotationYaw)) - moveStrafe * speed * -Math.sin(Math.toRadians(rotationYaw));
        return new double[] { posX, posZ };
    }

    private void sendPackets(final double x, final double y, final double z, final boolean teleport) {
        final Vec3d vec = new Vec3d(x, y, z);
        final Vec3d position = CCPacketFly.mc.player.getPositionVector().add(vec);
        final Vec3d outOfBoundsVec = this.outOfBoundsVec(position);
        this.packetSender((CPacketPlayer)new CPacketPlayer.Position(position.x, position.y, position.z, CCPacketFly.mc.player.onGround));
        this.packetSender((CPacketPlayer)new CPacketPlayer.Position(outOfBoundsVec.x, outOfBoundsVec.y, outOfBoundsVec.z, CCPacketFly.mc.player.onGround));
        this.teleportPacket(position, teleport);
    }

    private void teleportPacket(final Vec3d pos, final boolean shouldTeleport) {
        if (shouldTeleport) {
            CCPacketFly.mc.player.connection.sendPacket((Packet)new CPacketConfirmTeleport(++this.teleportID));
            this.teleportmap.put(this.teleportID, new IDtime(pos, new Timer()));
        }
    }

    private Vec3d outOfBoundsVec(final Vec3d position) {
        return position.add(0.0, 1337.0, 0.0);
    }

    private void packetSender(final CPacketPlayer packet) {
        this.packets.add(packet);
        CCPacketFly.mc.player.connection.sendPacket((Packet)packet);
    }

    public static class IDtime
    {
        private final Vec3d pos;
        private final Timer timer;

        public IDtime(final Vec3d pos, final Timer timer) {
            this.pos = pos;
            (this.timer = timer).reset();
        }

        public Vec3d getPos() {
            return this.pos;
        }

        public Timer getTimer() {
            return this.timer;
        }
    }
}

