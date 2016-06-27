
package climateControl;

import com.Zeno410Utils.Accessor;
import com.Zeno410Utils.Named;
import climateControl.api.BiomeSettings;
import climateControl.api.CCDimensionSettings;
import climateControl.api.ClimateControlSettings;
import climateControl.api.DimensionalSettingsRegistry;
import climateControl.customGenLayer.GenLayerLowlandRiverMix;
import climateControl.customGenLayer.GenLayerRiverMixWrapper;
import climateControl.genLayerPack.GenLayerPack;
import climateControl.generator.CorrectedContinentsGenerator;
import climateControl.generator.OneSixCompatibleGenerator;
import climateControl.generator.TestGeneratorPair;
import climateControl.generator.VanillaCompatibleGenerator;
import com.Zeno410Utils.ConfigManager;
import climateControl.utils.BiomeConfigManager;
import com.Zeno410Utils.Zeno410Logger;
import com.Zeno410Utils.ChunkGeneratorExtractor;
import com.Zeno410Utils.ChunkLister;
import com.Zeno410Utils.PlaneLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Logger;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.feature.WorldGeneratorBonusChest;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerRiverMix;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraft.world.gen.structure.MapGenVillage;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.util.math.BlockPos;
//import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.event.world.WorldEvent;

/**
 *
 * @author Zeno410
 */
public class DimensionManager {
    public static Logger logger = new Zeno410Logger("DimensionManager").logger();

    private Accessor<GenLayerRiverMix,GenLayerPack> riverMixBiome =
            new Accessor<GenLayerRiverMix,GenLayerPack>("field_75910_b");

    private final ClimateControlSettings newSettings;
    private final CCDimensionSettings dimensionSettings;
    private final Named<ClimateControlSettings> masterSettings;
    private GenLayerUpdater genLayerUpdater = new GenLayerUpdater();

    public DimensionManager(Named<ClimateControlSettings> masterSettings,CCDimensionSettings dimensionSettings,MinecraftServer server) {
        this.masterSettings = masterSettings;
        this.newSettings = masterSettings.object;
        this.dimensionSettings = dimensionSettings;
        if (server == null) {
            this.configDirectory= null;
            this.suggestedConfigFile = null;
            return;
        }
        this.configDirectory= server.getFile("config");
        this.suggestedConfigFile = new File(configDirectory,"geographicraft.cfg");
    }
    
    private GenLayerRiverMix patchedGenLayer(ClimateControlSettings settings,
            WorldType worldType,
            long worldSeed) {

        //logger.info("patching GenLayer: world seed "+worldSeed+"world type "+worldType.getWorldTypeName());
        for (BiomeSettings biomeSettings: settings.biomeSettings()) {
            //biomeSettings.report();
        }
        if (ignore(worldType,settings)) return null;
        if (settings.noGenerationChanges.value()) {
            if (settings.oneSixCompatibility.value()) {
                return new OneSixCompatibleGenerator(settings).fromSeed(worldSeed,worldType);
            } else {
                return null;
            }
        }
        // check settings
        new SettingsTester().test(settings);
        GenLayerRiverMix newMix = null;
        //logger.info("world seed " + worldSeed);
        if (settings.vanillaLandAndClimate.value()) {
             newMix = new VanillaCompatibleGenerator(settings).fromSeed(worldSeed,worldType);
        } else {
             newMix = new CorrectedContinentsGenerator(settings,this.configDirectory.getParentFile()).fromSeed(worldSeed,worldType);
        }
        GenLayer oldGen = null;//riverMixBiome.get(activeRiverMix);
        GenLayer newGen = riverMixBiome.get(newMix);
        TestGeneratorPair pair = new TestGeneratorPair(oldGen,newGen);
        while (true) {
            //logger.info(pair.description());
            if (!pair.hasNext()) break;
            pair = pair.next();
        }
        if (newMix instanceof GenLayerLowlandRiverMix) {
            ((GenLayerLowlandRiverMix)newMix).setMaxChasm(settings.maxRiverChasm.value().floatValue());
        }
        for (BiomeSettings biomeSettings: settings.biomeSettings()) {
            //biomeSettings.report();
        }
        return newMix;
    }

    private ClimateControlSettings newSettings() {
        return newSettings;
    }

    //private GenLayerRiverMixWrapper riverLayerWrapper = new GenLayerRiverMixWrapper(0L);

    private HashMap<Integer,ClimateControlSettings> dimensionalSettings = new HashMap<Integer,ClimateControlSettings>();
    private HashMap<Integer,GenLayerRiverMixWrapper> wrappers = new HashMap<Integer,GenLayerRiverMixWrapper>();

    private GenLayerRiverMixWrapper riverLayerWrapper(int dimension) {
        GenLayerRiverMixWrapper result = wrappers.get(dimension);
        if (result == null) {
            result = new GenLayerRiverMixWrapper(0L);
            result.setOriginal(original);
            wrappers.put(dimension,result);
        }
        return result;
    }

    private final File suggestedConfigFile;
    private final File configDirectory;
    
    private BiomeConfigManager addonConfigManager =
            new BiomeConfigManager("GeographiCraft");

    private ClimateControlSettings defaultSettings(MinecraftFilesAccess dimension, boolean newWorld) {
        ClimateControlSettings result = defaultSettings(newWorld);
        //logger.info(dimension.baseDirectory().getAbsolutePath());
        if (!dimension.baseDirectory().exists()) {
            dimension.baseDirectory().mkdir();
            if (!dimension.baseDirectory().exists()) {
            }
        }
        /*Configuration workingConfig = new Configuration(suggestedConfigFile);
        workingConfig.load();
        ConfigManager<ClimateControlSettings> workingConfigManager = new ConfigManager<ClimateControlSettings>(
        workingConfig,result,suggestedConfigFile);
        workingConfigManager.setWorldFile(dimension.baseDirectory());
        workingConfigManager.saveWorldSpecific();*/

        addonConfigManager.saveConfigs(configDirectory, dimension.configDirectory(), masterSettings);

        for (Named<BiomeSettings> addonSetting: result.registeredBiomeSettings()) {
            addonConfigManager.initializeConfig(addonSetting, configDirectory);
            addonConfigManager.updateConfig(addonSetting, configDirectory, dimension.configDirectory());
            if (newWorld) {
                addonSetting.object.onNewWorld();
                addonConfigManager.saveConfigs(this.configDirectory, dimension.configDirectory(), addonSetting);
            }
        }
        return result;
    }

    private ClimateControlSettings defaultSettings(boolean newWorld) {
        ClimateControlSettings result = new ClimateControlSettings();
        Named<ClimateControlSettings> namedResult = Named.from(ClimateControl.geographicraftConfigName, result);
        addonConfigManager.initializeConfig(namedResult, configDirectory);
        result.setDefaults(configDirectory);
        for (Named<BiomeSettings> addonSetting: result.registeredBiomeSettings()) {
            if (newWorld) addonSetting.object.onNewWorld();
            addonConfigManager.initializeConfig(addonSetting, configDirectory);
            if (newWorld) addonSetting.object.onNewWorld();
            addonSetting.object.setNativeBiomeIDs(configDirectory);
        }
        return result;
    }

    private ClimateControlSettings dimensionalSettings(DimensionAccess dimension, boolean newWorld) {
        ClimateControlSettings result = dimensionalSettings.get(dimension.dimension);
        if (result == null) {
            result = defaultSettings(dimension,newWorld);
            DimensionalSettingsRegistry.instance.modify(dimension.dimension, result);
            dimensionalSettings.put(dimension.dimension,result);
        }
        return result;

    }

    private GenLayer original;
    //GenLayer modified;
    /*GenLayer [] modifiedGenerators(long worldSeed) {
        GenLayer voronoi = new GenLayerVoronoiZoom(0L,original);
        return new GenLayer[] {original,voronoi,original};
    }

    GenLayer [] originalGenerators(long worldSeed) {
        GenLayer voronoi = new GenLayerVoronoiZoom(0L,original);
        return new GenLayer[] {modified,voronoi,modified};
    }*/

    public void onBiomeGenInit(WorldTypeEvent.InitBiomeGens event) {
        // skip if ignoring

        ClimateControlSettings generationSettings = defaultSettings(true);
        // this only gets used for new worlds,
        //when WorldServer is initializing and there are no spawn chunks yet
        generationSettings.onNewWorld();
        if (this.ignore(event.getWorldType(),this.newSettings)){
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server== null) {
            original = event.getOriginalBiomeGens()[0];
            riverLayerWrapper(0).setOriginal(event.getOriginalBiomeGens()[0]);
            riverLayerWrapper(0).useOriginal();
        }

        // get overworld dimension;
        boolean newWorld = true;
        for (WorldServer worldServer: server.worldServers){
            if (worldServer.getTotalWorldTime()>0 ) newWorld = false;
        }
        //logger.info(worldType.getTranslateName());
        //logger.info(event.originalBiomeGens[0].toString());
        // if not a recognized world type ignore
        //logBiomes();
        //this.activeRiverMix = (GenLayerRiverMix)(event.originalBiomeGens[0]);

        original = event.getOriginalBiomeGens()[0];
        riverLayerWrapper(0).setOriginal(event.getOriginalBiomeGens()[0]);

        if (generationSettings.noGenerationChanges.value()) {
            event.setNewBiomeGens(riverLayerWrapper(0).modifiedGenerators()) ;
            riverLayerWrapper(0).useOriginal();
            return;
        } else {
            // continue below
        }
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            GenLayerRiverMix patched = patchedGenLayer(generationSettings, event.getWorldType(), event.getSeed());
            riverLayerWrapper(0).setRedirection(patched);
            event.setNewBiomeGens(riverLayerWrapper(0).modifiedGenerators());
            event.setResult(WorldTypeEvent.Result.ALLOW);
        } else {
            //riverLayerWrapper(0).useOriginal();
            //event.newBiomeGens = modifiedGenerators(event.seed);
        }
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        new HashMap<Integer,GenLayerRiverMixWrapper>();
        dimensionalSettings = new HashMap<Integer,ClimateControlSettings>();
    }

    private boolean ignore(WorldType considered, ClimateControlSettings settings) {
        if (considered == null) return true;
        if (!settings.suppressInStandardWorlds.value()) {
            if (considered.equals(WorldType.AMPLIFIED)) return false;
            if (considered.equals(WorldType.DEFAULT)) {
                return false;
            }
            if (considered.equals(WorldType.DEFAULT_1_1)) return false;
            if (considered.equals(WorldType.LARGE_BIOMES)) return false;
        }
        if (considered.equals(WorldType.FLAT)) return true;
        if (settings.interveneInBOPWorlds.value()) {
            if (considered.getWorldTypeName().equalsIgnoreCase("BIOMESOP")) return false;
        }
        if (settings.interveneInHighlandsWorlds.value()) {

            if (considered.getWorldTypeName().equalsIgnoreCase("Highlands")) return false;
            if (considered.getWorldTypeName().equalsIgnoreCase("HighlandsLB")) return false;
        }
        if (true) {
            if (considered.getWorldTypeName().equalsIgnoreCase("FWG")) return false;
        }

        if (considered.getWorldTypeName().equalsIgnoreCase("RTG")) return false;
        return true;
    }

    public void onCreateSpawn(WorldEvent.CreateSpawnPosition event) {
        WorldServer world = (WorldServer)(event.getWorld());
        if (ignore(world.getWorldType(),this.newSettings)) {
            return;
        }
        int dimension = world.provider.getDimension();
        if (!this.dimensionSettings.ccOnIn(dimension)) {
            if (!DimensionalSettingsRegistry.instance.useCCIn(dimension)) {
                return;
            }
        }// only change dimensions we're supposed to;
        onWorldLoad(event.getWorld());
        salvageSpawn(event.getWorld());
        if (event.getSettings().isBonusChestEnabled()) {
            Random rand = new Random(world.getSeed());
            WorldGeneratorBonusChest worldgeneratorbonuschest =new WorldGeneratorBonusChest();

            for (int i = 0; i < 100; ++i){
                int j = world.getWorldInfo().getSpawnX() + rand.nextInt(6+i/10) - rand.nextInt(6+i/10);
                int k = world.getWorldInfo().getSpawnZ() + rand.nextInt(6+i/10) - rand.nextInt(6+i/10);

                BlockPos topBlockSpot = new BlockPos(j,world.getActualHeight()-1,k);
                while (!world.getBlockState(topBlockSpot).getBlock().isBlockSolid(world, topBlockSpot, EnumFacing.UP)) {
                    topBlockSpot = topBlockSpot.down();
                }
                BlockPos above = topBlockSpot.up();

                if (world.getBlockState(above).getBlock().isAir(world.getBlockState(above),world, above)){
                    if (worldgeneratorbonuschest.generate(world, rand, above)) break;
                }
            }
        }
        event.setCanceled(true);
    }

    private HashSet<Integer> dimensionsDone = new HashSet<Integer>();

    public void onWorldLoad(World world) {
        //logger.info(world.provider.terrainType.getWorldTypeName()+ " "+ world.provider.dimensionId);
        if (dimensionsDone.contains(world.provider.getDimension())) return;
        dimensionsDone.add(world.provider.getDimension());
        if (ignore(world.getWorldType(),this.newSettings)) {
            return;
        }
        int dimension = world.provider.getDimension();
        //logger.info(""+this.dimensionSettings.ccOnIn(dimension));
        if (!this.dimensionSettings.ccOnIn(dimension)) {
            if (!DimensionalSettingsRegistry.instance.useCCIn(dimension)) {
                return;
            }
        }// only change dimensions we're supposed to;
        if (world.isRemote) {
            return;
        }
        // pull out the Chunk Generator
        // this whole business will crash if things aren't right. Probably the best behavior,
        // although a polite message might be appropriate

        // salvage spawn if new world

        // do nothing for client worlds

        if (!(world instanceof WorldServer)) return;

        WorldServer worldServer = (WorldServer)(world);
        DimensionAccess dimensionAccess = new DimensionAccess(dimension,worldServer);

        long worldSeed = world.getSeed();
        if (world instanceof WorldServer&&worldSeed!=0)  {
            ClimateControlSettings currentSettings = null;
            boolean newWorld = false;
            //logger.info("time "+world.getWorldInfo().getWorldTotalTime());
            if(world.getWorldInfo().getWorldTotalTime()<100) {
                // new world
                newWorld = true;
            }
            currentSettings = dimensionalSettings(dimensionAccess,newWorld);
            //logger.info(""+dimension +" "+ currentSettings.snowyIncidence.value() +" "+ currentSettings.coolIncidence.value());

            riverLayerWrapper(dimension).setOriginal(original);

            try {
                GenLayerRiverMix patched = patchedGenLayer(currentSettings,world.getWorldType(),worldSeed);
                if (patched != null) {
                    riverLayerWrapper(dimension).setRedirection(patched);
                    genLayerUpdater.update(this.riverLayerWrapper(dimension), world.provider);
                    this.riverLayerWrapper(dimension).lock(dimension, world,currentSettings);
                } else {
                    // lock manually
                    LockGenLayers biomeLocker = new LockGenLayers();
                    BiomeProvider chunkGenerator = world.getBiomeProvider();
                    Accessor<BiomeProvider,GenLayer> worldGenLayer =
                        new Accessor<BiomeProvider,GenLayer>("field_76944_d");
                    GenLayer toLock = worldGenLayer.get(chunkGenerator);
                    if (toLock instanceof GenLayerRiverMixWrapper) {
                       toLock = original;
                    }
                        Accessor<GenLayerRiverMix,GenLayer> riverMixBiome =
                        new Accessor<GenLayerRiverMix,GenLayer>("field_75910_b");
                        toLock = riverMixBiome.get((GenLayerRiverMix)toLock);
                    biomeLocker.lock(toLock, dimension, world, currentSettings);
                }
            if (currentSettings.vanillaLandAndClimate.value() == false){
                if (currentSettings.noGenerationChanges.value() == false) {
                    //logger.info(new ChunkGeneratorExtractor().extractFrom((WorldServer)world).toString());
                    try {
                        new ChunkGeneratorExtractor().impose((WorldServer) world, new MapGenVillage());
                    } catch (Exception e) {
                    } catch (NoClassDefFoundError e) {
                    }
                    if(world.getWorldInfo().getWorldTotalTime()<40000) {
                        ArrayList<PlaneLocation> existingChunks =
                        new ChunkLister().savedChunks(levelSavePath((WorldServer)world));
                        //logger.info("existing chunks:"+existingChunks.size());
                        //world.provider.worldChunkMgr.
                        //salvageSpawn(world);
                    }
                }
            }
            } catch (Exception e) {
                logger.info(e.toString());
                logger.info(e.getMessage());
            } catch (Error e) {
                logger.info(e.toString());
                logger.info(e.getMessage());
            }
            //logger.info("start rescued");
        } else {
            genLayerUpdater.update(this.riverLayerWrapper(dimension), world.provider);
            LockGenLayer.logger.info(world.toString());
            /*
            if (!(world instanceof WorldClient)) {
                logger.info("World Client problem");
                throw new RuntimeException();
                //logger.info("locking "+riverLayerWrapper.forLocking().toString());
                //this.biomeLocker.lock(riverMixBiome.get(activeRiverMix), dimension, this.servedWorlds.get(dimension),newSettings);
            } else {
                //logger.info("client world");
            }*/
            //this.riverMixBiome.setField(this.vanillaGenLayer,replacement);
            //this.riverMixRiver.setField(this.vanillaGenLayer,replacement);

        }
        //this.biomeLocker.showGenLayers(accessGenLayer.get(world.provider.worldChunkMgr));
    }

    private String controllingGenLayer(World world) {
        BiomeProvider chunkManager = world.getBiomeProvider();

        Accessor<BiomeProvider,GenLayer> worldGenLayer =
            new Accessor<BiomeProvider,GenLayer>("field_76944_d");

        return worldGenLayer.get(chunkManager).toString();

    }
   PlaneLocation lastTry = new PlaneLocation(Integer.MIN_VALUE,Integer.MIN_VALUE);


    private void salvageSpawn(World world) {
        WorldInfo info = world.getWorldInfo();
        int x= info.getSpawnX()/16*16 + this.newSettings().xSpawnOffset.value();
        int z= info.getSpawnZ()/16*16 + this.newSettings().zSpawnOffset.value();
        //x = x/16;
        //z = z/16;
        int move = 0;
        int ring = 0;
        int xMove = 0;
        int zMove = 0;
        int spawnX = 0;
        int spawnZ = 0;
        int spawnY = 0;
        Biome checkSpawn = world.getBiomeGenForCoords(new BlockPos(x,64,z));
        int nextTry = 50;
        int nextTryIncrement = 80;
        int nextTryStretch = 20;
        IChunkProvider chunkManager = world.getChunkProvider();
        ChunkProviderServer chunkServer = null;
        try {
            chunkServer = (ChunkProviderServer)chunkManager;
        } catch (ClassCastException e) {
            throw e;
        }
        int checked = 0;
        int rescueTries = 0;
        while(spawnY < 64) {
            if (newSettings.rescueSearchLimit.value() == rescueTries++) {
                // limit reached, give up and quit
                return;
            }
            //while (isSea(checkSpawn)) {
                //spiral out around spawn;
            if (checked > 50) {
                if (chunkServer != null) {
                    chunkServer.unloadAllChunks();
                    //chunkServer.unloadQueuedChunks();
                }
                checked = 0;
            }
            if (chunkServer != null) {
                //chunkServer.unloadChunksIfNotNearSpawn(checked, checked);
            }
            checked++;
                move ++;
                if (move>((ring)*(ring+1)*4)) {
                    ring ++;
                }
                // do 1 in 1000
                if (move<nextTry) continue;
                nextTryIncrement += nextTryStretch++;
                nextTry += nextTryIncrement;
                int inRing = move - (ring-1)*(ring)*4;
                // find which side of the ring we are on;
                // note inRing is 1-based not 0-base
                if (inRing > ring * 6) {
                    // left side
                    xMove = -ring;
                    zMove = ring - (inRing-ring*6) + 1;
                } else if (inRing > ring *4) {
                    zMove = ring;
                    xMove = ring - (inRing-ring*4) + 1;
                } else if (inRing > ring * 2) {
                    xMove = ring;
                    zMove = -ring + (inRing-ring*2) -1;
                } else {
                    zMove = -ring;
                    xMove = -ring + (inRing) -1;
                }
                IntCache.resetIntCache();
                logger.info("checking for spawn at "+ (x+xMove*16) + ","+ (z+zMove*16) + "move " + move
                        + " ring "+ ring + " inRing " + inRing+ " caches " + IntCache.getCacheSizes()
                        + " dimension " + world.provider.getDimension());
                checkSpawn = world.getBiomeGenForCoords(new BlockPos(x+xMove*16, 64, z+zMove*16));
            //}
            //int spawnY = checkSpawn.getHeightValue(8,8);
            spawnX = x+xMove*16;
            spawnZ = z+zMove*16;
            logger.info("setting spawn at "+ spawnX + ","+ spawnZ);
            if (checkSpawn == Biomes.MUSHROOM_ISLAND) continue;
            spawnY = world.getTopSolidOrLiquidBlock(new BlockPos(spawnX, 64, spawnZ)).getY()+1;
            PlaneLocation newLocation = new PlaneLocation(spawnX,spawnZ);
            if (newLocation.equals(lastTry)) break;
        }
        world.setSpawnPoint(new BlockPos(spawnX,spawnY,spawnZ));
    }

    private String levelSavePath(WorldServer world) {
        String result = "";
        result = world.getChunkSaveLocation().getAbsolutePath();
        return result;
    }
    private boolean hasOnlySea(Chunk tested) {
        byte [] biomes = tested.getBiomeArray();
        for (byte biome: biomes) {
            if (biome == 0) continue;
            if (biome == Biome.getIdForBiome(Biomes.DEEP_OCEAN)) continue;
            return false;
        }
        return true;
    }

    private boolean isSea(Biome tested) {
        if (tested == Biomes.OCEAN) return true;
        if (tested == Biomes.DEEP_OCEAN) return true;
        if (tested == Biomes.MUSHROOM_ISLAND) return true;
        return false;
    }
}
