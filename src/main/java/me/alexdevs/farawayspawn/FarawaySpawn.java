package me.alexdevs.farawayspawn;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

@Mod("farawayspawn")
public class FarawaySpawn {
    private static final Logger LOGGER = LogManager.getLogger();

    public FarawaySpawn() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void playerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        MinecraftServer server = event.getPlayer().getServer();
        if (server == null)
            return;

        ServerPlayerEntity player = server.getPlayerList().getPlayer(event.getPlayer().getUUID());

        if (player != null) {
            CompoundNBT data = player.getPersistentData();
            if (!data.contains("farawayspawn_firstspawn")) {
                LOGGER.info("Setting first spawn point of new player");

                BlockPos blockPos = findGoodSpot(server.overworld());
                // World, BlockPos, Angle, Forced, AnnounceMessage
                player.setRespawnPosition(server.overworld().dimension(), blockPos, 0f, true, false);
                player.teleportTo(server.overworld(), blockPos.getX(), blockPos.getY(), blockPos.getZ(), player.yRot, player.xRot);

                data.putBoolean("farawayspawn_firstspawn", true);
            }
        }
    }

    public BlockPos generateRandomCoordinates(ServerWorld world, double distance) {
        double dist = world.random.nextDouble() * distance;
        double angle = world.random.nextDouble() * Math.PI * 2d;

        int x = (int) Math.floor(Math.cos(angle) * dist);
        int y = 256;
        int z = (int) Math.floor(Math.sin(angle) * dist);

        return new BlockPos(x, y, z);
    }

    public BlockPos findGoodSpot(ServerWorld world) {
        WorldBorder border = world.getWorldBorder();
        BlockPos pos = generateRandomCoordinates(world, border.getSize());

        if(!border.isWithinBounds(pos)) {
            return findGoodSpot(world);
        }

        Optional<RegistryKey<Biome>> biomeKey = world.getBiomeName(pos);
        if(biomeKey.isPresent() && biomeKey.get().location().getPath().contains("ocean")) {
            return findGoodSpot(world);
        }

        world.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.HEIGHTMAPS);
        BlockPos hmPos = world.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos);

        return hmPos.above();
    }
}
