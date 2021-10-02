package me.earth.phobos.features.modules.movement;
import me.earth.phobos.util.Timer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import me.earth.phobos.event.events.PacketEvent;
import me.earth.phobos.features.command.Command;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.CPacketPlayer;
import me.earth.phobos.features.setting.Bind;
import me.earth.phobos.features.setting.Setting;
import me.earth.phobos.features.modules.Module;

public class BlockPhase extends Module
{
    private static BlockPhase INSTANCE;
    public Setting<Boolean> twodelay;
    public Setting<Integer> tickDelay;
    public Setting<Double> speed;
    public Setting<Bind> left;
    public Setting<Bind> right;
    public Setting<Bind> down;
    public Setting<Bind> up;

    public BlockPhase() {
        super("BlockPhase", "phase && pfly", Category.MOVEMENT, true, false, false);
        final Setting < Boolean > twodelay = this.register ( new Setting <> ( "2Delay" , true ) );
        final Setting < Integer > tickDelay = this.register ( new Setting <> ( "TickDelay" , 1 , 0 , 40 ) );
        final Setting < Double > speed = this.register ( new Setting <> ( "Speed" , 6.0 , 0.0 , 6.0 ) );
        this.left = new Setting<Bind>("Left", new Bind(203));
        this.right = new Setting<Bind>("Right", new Bind(205));
        this.down = new Setting<Bind>("Down", new Bind(208));
        this.up = new Setting<Bind>("Up", new Bind(200));
        this.setInstance();
    }
    public static BlockPhase getInstance() {
        if (BlockPhase.INSTANCE == null) {
            BlockPhase.INSTANCE = new BlockPhase();
        }
        return BlockPhase.INSTANCE;
    }

    private void setInstance() {
        BlockPhase.INSTANCE = this;
    }

    @Override
    public void onUpdate() {
        final long last = 0L;
        try {
            BlockPhase.mc.player.noClip = true;
            if (this.tickDelay.getValue() > 0 && BlockPhase.mc.player.ticksExisted % this.tickDelay.getValue() != 0 && this.twodelay.getValue()) {
                return;
            }
            double yaw = (BlockPhase.mc.player.rotationYaw + 90.0f) * 1.0f;
            double dO_numer = 0.0;
            double dO_denom = 0.0;
            if (BlockPhase.mc.gameSettings.keyBindLeft.isKeyDown()) {
                dO_numer -= 90.0;
                ++dO_denom;
            }
            if (BlockPhase.mc.gameSettings.keyBindRight.isKeyDown()) {
                dO_numer += 90.0;
                ++dO_denom;
            }
            if (BlockPhase.mc.gameSettings.keyBindBack.isKeyDown()) {
                dO_numer += 180.0;
                ++dO_denom;
            }
            if (BlockPhase.mc.gameSettings.keyBindForward.isKeyDown()) {
                ++dO_denom;
            }
            if (dO_denom > 0.0) {
                yaw += dO_numer / dO_denom % 361.0;
            }
            if (yaw < 0.0) {
                yaw = 360.0 - yaw;
            }
            if (yaw > 360.0) {
                yaw %= 361.0;
            }
            final double xDir = Math.cos(Math.toRadians(yaw));
            final double zDir = Math.sin(Math.toRadians(yaw));
            if (BlockPhase.mc.gameSettings.keyBindForward.isKeyDown() || BlockPhase.mc.gameSettings.keyBindLeft.isKeyDown() || BlockPhase.mc.gameSettings.keyBindRight.isKeyDown() || BlockPhase.mc.gameSettings.keyBindBack.isKeyDown()) {
                BlockPhase.mc.player.motionX = xDir * (this.speed.getValue() / 100.0);
                BlockPhase.mc.player.motionZ = zDir * (this.speed.getValue() / 100.0);
            }
            BlockPhase.mc.player.motionY = 0.0;
            final boolean yes = false;
            BlockPhase.mc.player.noClip = true;
            if (yes) {
                BlockPhase.mc.getConnection().sendPacket((Packet)new CPacketPlayer.Position(BlockPhase.mc.player.posX + BlockPhase.mc.player.motionX, BlockPhase.mc.player.posY + ((BlockPhase.mc.player.posY < -0.98) ? (this.speed.getValue() / 100.0) : 0.0) + (BlockPhase.mc.gameSettings.keyBindJump.isKeyDown() ? (this.speed.getValue() / 100.0) : 0.0) - (BlockPhase.mc.gameSettings.keyBindSneak.isKeyDown() ? (this.speed.getValue() / 100.0) : 0.0), BlockPhase.mc.player.posZ + BlockPhase.mc.player.motionZ, false));
            }
            BlockPhase.mc.player.noClip = true;
            if (yes) {
                BlockPhase.mc.getConnection().sendPacket((Packet)new CPacketPlayer.Position(BlockPhase.mc.player.posX + BlockPhase.mc.player.motionX, BlockPhase.mc.player.posY - 42069.0, BlockPhase.mc.player.posZ + BlockPhase.mc.player.motionZ, true));
            }
            final double dx = 0.0;
            double dy = 0.0;
            final double dz = 0.0;
            if (BlockPhase.mc.gameSettings.keyBindSneak.isKeyDown()) {
                dy = -0.0625;
            }
            if (BlockPhase.mc.gameSettings.keyBindJump.isKeyDown()) {
                dy = 0.0625;
            }
                BlockPhase.mc.player.setLocationAndAngles(BlockPhase.mc.player.posX, BlockPhase.mc.player.posY, BlockPhase.mc.player.posZ, BlockPhase.mc.player.rotationYaw, BlockPhase.mc.player.rotationPitch);
            BlockPhase.mc.getConnection().sendPacket((Packet)new CPacketPlayer.Position(BlockPhase.mc.player.posX, BlockPhase.mc.player.posY, BlockPhase.mc.player.posZ, false));
        }
        catch (Exception e) {
            Command.sendMessage("§cException caught: " + e.getMessage());
            this.disable();
        }
    }

    @Override
    public void onDisable() {
        if (BlockPhase.mc.player != null) {
            BlockPhase.mc.player.noClip = false;
        }
    }

    @SubscribeEvent
    public void onPacketReceive(final PacketEvent.Receive event) {
        if (event.getPacket() instanceof SPacketPlayerPosLook) {
            final SPacketPlayerPosLook pak = event.getPacket();
            pak.yaw = BlockPhase.mc.player.rotationYaw;
            pak.pitch = BlockPhase.mc.player.rotationPitch;
        }
        if (event.getPacket() instanceof SPacketPlayerPosLook) {
            final SPacketPlayerPosLook pak = event.getPacket();
            double dx = Math.abs(pak.getFlags().contains(SPacketPlayerPosLook.EnumFlags.X) ? pak.x : (BlockPhase.mc.player.posX - pak.x));
            final double dy = Math.abs(pak.getFlags().contains(SPacketPlayerPosLook.EnumFlags.Y) ? pak.y : (BlockPhase.mc.player.posY - pak.y));
            double dz = Math.abs(pak.getFlags().contains(SPacketPlayerPosLook.EnumFlags.Z) ? pak.z : (BlockPhase.mc.player.posZ - pak.z));
            if (dx < 0.001) {
                dx = 0.0;
            }
            if (dz < 0.001) {
                dz = 0.0;
            }
            if (pak.yaw != BlockPhase.mc.player.rotationYaw || pak.pitch != BlockPhase.mc.player.rotationPitch) {
                pak.yaw = BlockPhase.mc.player.rotationYaw;
                pak.pitch = BlockPhase.mc.player.rotationPitch;
            }
        }
    }

    public static BlockPos getPlayerPos() {
        return new BlockPos(Math.floor(BlockPhase.mc.player.posX), Math.floor(BlockPhase.mc.player.posY), Math.floor(BlockPhase.mc.player.posZ));
    }

    static {
        BlockPhase.INSTANCE = new BlockPhase();
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



