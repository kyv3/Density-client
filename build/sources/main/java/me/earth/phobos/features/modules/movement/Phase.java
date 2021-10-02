package me.earth.phobos.features.modules.movement;

import io.netty.util.internal.ConcurrentSet;
import me.earth.phobos.event.events.MoveEvent;
import me.earth.phobos.event.events.PacketEvent;
import me.earth.phobos.event.events.PushEvent;
import me.earth.phobos.event.events.UpdateWalkingPlayerEvent;
import me.earth.phobos.features.modules.Module;
import me.earth.phobos.features.setting.Setting;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Set;

public class Phase
        extends Module {
    private static Phase INSTANCE = new Phase( );
    private final Set < CPacketPlayer > packets = new ConcurrentSet <> ( );
    public Setting < Mode > mode = this.register ( new Setting <> ( "Mode" , Mode.PACKETFLY ) );
    public Setting < PacketFlyMode > type = this.register ( new Setting < Object > ( "Type" , PacketFlyMode.SETBACK , v -> this.mode.getValue ( ) == Mode.PACKETFLY ) );
    public Setting < Integer > yMove = this.register ( new Setting < Object > ( "YMove" , 625 , 1 , 1000 , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK , "YMovement speed." ) );
    public Setting < Boolean > extra = this.register ( new Setting < Object > ( "ExtraPacket" , Boolean.TRUE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Integer > offset = this.register ( new Setting < Object > ( "Offset" , 1337 , - 1337 , 1337 , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK && this.extra.getValue ( ) , "Up speed." ) );
    public Setting < Boolean > fallPacket = this.register ( new Setting < Object > ( "FallPacket" , Boolean.TRUE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > teleporter = this.register ( new Setting < Object > ( "Teleport" , Boolean.TRUE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > boundingBox = this.register ( new Setting < Object > ( "BoundingBox" , Boolean.TRUE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Integer > teleportConfirm = this.register ( new Setting < Object > ( "Confirm" , 2 , 0 , 4 , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > ultraPacket = this.register ( new Setting < Object > ( "DoublePacket" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > updates = this.register ( new Setting < Object > ( "Update" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > setOnMove = this.register ( new Setting < Object > ( "SetMove" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > cliperino = this.register ( new Setting < Object > ( "NoClip" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK && this.setOnMove.getValue ( ) ) );
    public Setting < Boolean > scanPackets = this.register ( new Setting < Object > ( "ScanPackets" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > resetConfirm = this.register ( new Setting < Object > ( "Reset" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > posLook = this.register ( new Setting < Object > ( "PosLook" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK ) );
    public Setting < Boolean > cancel = this.register ( new Setting < Object > ( "Cancel" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK && this.posLook.getValue ( ) ) );
    public Setting < Boolean > cancelType = this.register ( new Setting < Object > ( "SetYaw" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK && this.posLook.getValue ( ) && this.cancel.getValue ( ) ) );
    public Setting < Boolean > onlyY = this.register ( new Setting < Object > ( "OnlyY" , Boolean.FALSE , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK && this.posLook.getValue ( ) ) );
    public Setting < Integer > cancelPacket = this.register ( new Setting < Object > ( "Packets" , 20 , 0 , 20 , v -> this.mode.getValue ( ) == Mode.PACKETFLY && this.type.getValue ( ) == PacketFlyMode.SETBACK && this.posLook.getValue ( ) ) );
    private boolean teleport = true;
    private int teleportIds;
    private int posLookPackets;

    public
    Phase ( ) {
        super ( "Phase" , "Makes you able to phase through blocks." , Module.Category.MOVEMENT , true , false , false );
        this.setInstance ( );
    }

    public static Phase getInstance ( ) {
        if ( INSTANCE == null ) {
            INSTANCE = new Phase( );
        }
        return INSTANCE;
    }

    private
    void setInstance ( ) {
        INSTANCE = this;
    }

    @Override
    public
    void onDisable ( ) {
        this.packets.clear ( );
        this.posLookPackets = 0;
        if ( BlockPhase.mc.player != null ) {
            if ( this.resetConfirm.getValue ( ) ) {
                this.teleportIds = 0;
            }
            BlockPhase.mc.player.noClip = false;
        }
    }

    @Override
    public
    String getDisplayInfo ( ) {
        return this.mode.currentEnumName ( );
    }

    @SubscribeEvent
    public
    void onMove ( MoveEvent event ) {
        if ( this.setOnMove.getValue ( ) && this.type.getValue ( ) == PacketFlyMode.SETBACK && event.getStage ( ) == 0 && ! mc.isSingleplayer ( ) && this.mode.getValue ( ) == Mode.PACKETFLY ) {
            event.setX ( BlockPhase.mc.player.motionX );
            event.setY ( BlockPhase.mc.player.motionY );
            event.setZ ( BlockPhase.mc.player.motionZ );
            if ( this.cliperino.getValue ( ) ) {
                BlockPhase.mc.player.noClip = true;
            }
        }
        if ( this.type.getValue ( ) == PacketFlyMode.NONE || event.getStage ( ) != 0 || mc.isSingleplayer ( ) || this.mode.getValue ( ) != Mode.PACKETFLY ) {
            return;
        }
        if ( ! this.boundingBox.getValue ( ) && ! this.updates.getValue ( ) ) {
            this.doPhase ( event );
        }
    }

    @SubscribeEvent
    public
    void onPush ( PushEvent event ) {
        if ( event.getStage ( ) == 1 && this.type.getValue ( ) != PacketFlyMode.NONE ) {
            event.setCanceled ( true );
        }
    }

    @SubscribeEvent
    public
    void onMove ( UpdateWalkingPlayerEvent event ) {
        if ( BlockPhase.fullNullCheck ( ) || event.getStage ( ) != 0 || this.type.getValue ( ) != PacketFlyMode.SETBACK || this.mode.getValue ( ) != Mode.PACKETFLY ) {
            return;
        }
        if ( this.boundingBox.getValue ( ) ) {
            this.doBoundingBox ( );
        } else if ( this.updates.getValue ( ) ) {
            this.doPhase ( null );
        }
    }

    private
    void doPhase ( MoveEvent event ) {
        if ( this.type.getValue ( ) == PacketFlyMode.SETBACK && ! this.boundingBox.getValue ( ) ) {
            double[] dirSpeed = this.getMotion ( this.teleport ? (double) this.yMove.getValue ( ) / 10000.0 : (double) ( this.yMove.getValue ( ) - 1 ) / 10000.0 );
            double posX = BlockPhase.mc.player.posX + dirSpeed[0];
            double posY = BlockPhase.mc.player.posY + ( BlockPhase.mc.gameSettings.keyBindJump.isKeyDown ( ) ? ( this.teleport ? (double) this.yMove.getValue ( ) / 10000.0 : (double) ( this.yMove.getValue ( ) - 1 ) / 10000.0 ) : 1.0E-8 ) - ( BlockPhase.mc.gameSettings.keyBindSneak.isKeyDown ( ) ? ( this.teleport ? (double) this.yMove.getValue ( ) / 10000.0 : (double) ( this.yMove.getValue ( ) - 1 ) / 10000.0 ) : 2.0E-8 );
            double posZ = BlockPhase.mc.player.posZ + dirSpeed[1];
            CPacketPlayer.PositionRotation packetPlayer = new CPacketPlayer.PositionRotation ( posX , posY , posZ , BlockPhase.mc.player.rotationYaw , BlockPhase.mc.player.rotationPitch , false );
            this.packets.add ( packetPlayer );
            BlockPhase.mc.player.connection.sendPacket ( packetPlayer );
            if ( this.teleportConfirm.getValue ( ) != 3 ) {
                BlockPhase.mc.player.connection.sendPacket ( new CPacketConfirmTeleport ( this.teleportIds - 1 ) );
                ++ this.teleportIds;
            }
            if ( this.extra.getValue ( ) ) {
                CPacketPlayer.PositionRotation packet = new CPacketPlayer.PositionRotation ( BlockPhase.mc.player.posX , (double) this.offset.getValue ( ) + BlockPhase.mc.player.posY , BlockPhase.mc.player.posZ , BlockPhase.mc.player.rotationYaw , BlockPhase.mc.player.rotationPitch , true );
                this.packets.add ( packet );
                BlockPhase.mc.player.connection.sendPacket ( packet );
            }
            if ( this.teleportConfirm.getValue ( ) != 1 ) {
                BlockPhase.mc.player.connection.sendPacket ( new CPacketConfirmTeleport ( this.teleportIds + 1 ) );
                ++ this.teleportIds;
            }
            if ( this.ultraPacket.getValue ( ) ) {
                CPacketPlayer.PositionRotation packet2 = new CPacketPlayer.PositionRotation ( posX , posY , posZ , BlockPhase.mc.player.rotationYaw , BlockPhase.mc.player.rotationPitch , false );
                this.packets.add ( packet2 );
                BlockPhase.mc.player.connection.sendPacket ( packet2 );
            }
            if ( this.teleportConfirm.getValue ( ) == 4 ) {
                BlockPhase.mc.player.connection.sendPacket ( new CPacketConfirmTeleport ( this.teleportIds ) );
                ++ this.teleportIds;
            }
            if ( this.fallPacket.getValue ( ) ) {
                BlockPhase.mc.player.connection.sendPacket ( new CPacketEntityAction ( BlockPhase.mc.player , CPacketEntityAction.Action.START_FALL_FLYING ) );
            }
            BlockPhase.mc.player.setPosition ( posX , posY , posZ );
            this.teleport = ! this.teleporter.getValue ( ) || ! this.teleport;
            if ( event != null ) {
                event.setX ( 0.0 );
                event.setY ( 0.0 );
                event.setX ( 0.0 );
            } else {
                BlockPhase.mc.player.motionX = 0.0;
                BlockPhase.mc.player.motionY = 0.0;
                BlockPhase.mc.player.motionZ = 0.0;
            }
        }
    }

    private
    void doBoundingBox ( ) {
        double[] dirSpeed = this.getMotion ( this.teleport ? (double) 0.0225f : (double) 0.0224f );
        BlockPhase.mc.player.connection.sendPacket ( new CPacketPlayer.PositionRotation ( BlockPhase.mc.player.posX + dirSpeed[0] , BlockPhase.mc.player.posY + ( BlockPhase.mc.gameSettings.keyBindJump.isKeyDown ( ) ? ( this.teleport ? 0.0625 : 0.0624 ) : 1.0E-8 ) - ( BlockPhase.mc.gameSettings.keyBindSneak.isKeyDown ( ) ? ( this.teleport ? 0.0625 : 0.0624 ) : 2.0E-8 ) , BlockPhase.mc.player.posZ + dirSpeed[1] , BlockPhase.mc.player.rotationYaw , BlockPhase.mc.player.rotationPitch , false ) );
        BlockPhase.mc.player.connection.sendPacket ( new CPacketPlayer.PositionRotation ( BlockPhase.mc.player.posX , - 1337.0 , BlockPhase.mc.player.posZ , BlockPhase.mc.player.rotationYaw , BlockPhase.mc.player.rotationPitch , true ) );
        BlockPhase.mc.player.connection.sendPacket ( new CPacketEntityAction ( BlockPhase.mc.player , CPacketEntityAction.Action.START_FALL_FLYING ) );
        BlockPhase.mc.player.setPosition ( BlockPhase.mc.player.posX + dirSpeed[0] , BlockPhase.mc.player.posY + ( BlockPhase.mc.gameSettings.keyBindJump.isKeyDown ( ) ? ( this.teleport ? 0.0625 : 0.0624 ) : 1.0E-8 ) - ( BlockPhase.mc.gameSettings.keyBindSneak.isKeyDown ( ) ? ( this.teleport ? 0.0625 : 0.0624 ) : 2.0E-8 ) , BlockPhase.mc.player.posZ + dirSpeed[1] );
        this.teleport = ! this.teleport;
        BlockPhase.mc.player.motionZ = 0.0;
        BlockPhase.mc.player.motionY = 0.0;
        BlockPhase.mc.player.motionX = 0.0;
        BlockPhase.mc.player.noClip = this.teleport;
    }

    @SubscribeEvent
    public
    void onPacketReceive ( PacketEvent.Receive event ) {
        if ( this.posLook.getValue ( ) && event.getPacket ( ) instanceof SPacketPlayerPosLook ) {
            SPacketPlayerPosLook packet = event.getPacket ( );
            if ( BlockPhase.mc.player.isEntityAlive ( ) && BlockPhase.mc.world.isBlockLoaded ( new BlockPos ( BlockPhase.mc.player.posX , BlockPhase.mc.player.posY , BlockPhase.mc.player.posZ ) ) && ! ( BlockPhase.mc.currentScreen instanceof GuiDownloadTerrain ) ) {
                if ( this.teleportIds <= 0 ) {
                    this.teleportIds = packet.getTeleportId ( );
                }
                if ( this.cancel.getValue ( ) && this.cancelType.getValue ( ) ) {
                    packet.yaw = BlockPhase.mc.player.rotationYaw;
                    packet.pitch = BlockPhase.mc.player.rotationPitch;
                    return;
                }
                if ( ! ( ! this.cancel.getValue ( ) || this.posLookPackets < this.cancelPacket.getValue ( ) || this.onlyY.getValue ( ) && ( BlockPhase.mc.gameSettings.keyBindForward.isKeyDown ( ) || BlockPhase.mc.gameSettings.keyBindRight.isKeyDown ( ) || BlockPhase.mc.gameSettings.keyBindLeft.isKeyDown ( ) || BlockPhase.mc.gameSettings.keyBindBack.isKeyDown ( ) ) ) ) {
                    this.posLookPackets = 0;
                    event.setCanceled ( true );
                }
                ++ this.posLookPackets;
            }
        }
    }

    @SubscribeEvent
    public
    void onPacketReceive ( PacketEvent.Send event ) {
        if ( this.scanPackets.getValue ( ) && event.getPacket ( ) instanceof CPacketPlayer ) {
            CPacketPlayer packetPlayer = event.getPacket ( );
            if ( this.packets.contains ( packetPlayer ) ) {
                this.packets.remove ( packetPlayer );
            } else {
                event.setCanceled ( true );
            }
        }
    }

    private
    double[] getMotion ( double speed ) {
        float moveForward = BlockPhase.mc.player.movementInput.moveForward;
        float moveStrafe = BlockPhase.mc.player.movementInput.moveStrafe;
        float rotationYaw = BlockPhase.mc.player.prevRotationYaw + ( BlockPhase.mc.player.rotationYaw - BlockPhase.mc.player.prevRotationYaw ) * mc.getRenderPartialTicks ( );
        if ( moveForward != 0.0f ) {
            if ( moveStrafe > 0.0f ) {
                rotationYaw += (float) ( moveForward > 0.0f ? - 45 : 45 );
            } else if ( moveStrafe < 0.0f ) {
                rotationYaw += (float) ( moveForward > 0.0f ? 45 : - 45 );
            }
            moveStrafe = 0.0f;
            if ( moveForward > 0.0f ) {
                moveForward = 1.0f;
            } else if ( moveForward < 0.0f ) {
                moveForward = - 1.0f;
            }
        }
        double posX = (double) moveForward * speed * - Math.sin ( Math.toRadians ( rotationYaw ) ) + (double) moveStrafe * speed * Math.cos ( Math.toRadians ( rotationYaw ) );
        double posZ = (double) moveForward * speed * Math.cos ( Math.toRadians ( rotationYaw ) ) - (double) moveStrafe * speed * - Math.sin ( Math.toRadians ( rotationYaw ) );
        return new double[]{posX , posZ};
    }

    public
    enum PacketFlyMode {
        NONE,
        SETBACK

    }

    public
    enum Mode {
        PACKETFLY

    }
}

