package climateControl.genLayerPack;
import com.Zeno410Utils.Zeno410Logger;
import java.util.logging.Logger;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

import net.minecraft.world.WorldType;
public class GenLayerBiome extends GenLayerPack
{
    private static Logger logger = new Zeno410Logger("GenLayerBiome").logger();
    private Biome[] field_151623_c;
    private Biome[] field_151621_d;
    private Biome[] field_151622_e;
    private Biome[] field_151620_f;
    private static final String __OBFID = "CL_00000555";

    public GenLayerBiome(long par1, GenLayer par3GenLayer, WorldType worldType)//, WorldType par4WorldType)
    {
        super(par1);
        this.field_151623_c = new Biome[] {Biomes.DESERT, Biomes.DESERT, Biomes.DESERT, Biomes.SAVANNA, Biomes.SAVANNA, Biomes.PLAINS};
        this.field_151621_d = new Biome[] {Biomes.FOREST, Biomes.ROOFED_FOREST, Biomes.EXTREME_HILLS, Biomes.PLAINS, Biomes.BIRCH_FOREST, Biomes.SWAMPLAND};
        this.field_151622_e = new Biome[] {Biomes.FOREST, Biomes.EXTREME_HILLS, Biomes.TAIGA, Biomes.PLAINS};
        this.field_151620_f = new Biome[] {Biomes.ICE_PLAINS, Biomes.ICE_PLAINS, Biomes.ICE_PLAINS, Biomes.COLD_TAIGA};
        this.parent = par3GenLayer;

        if (worldType == WorldType.DEFAULT_1_1)
        {
            this.field_151623_c = new Biome[] {Biomes.DESERT, Biomes.FOREST, Biomes.EXTREME_HILLS, Biomes.SWAMPLAND, Biomes.PLAINS, Biomes.TAIGA};
        }
    }

    /**
     * Returns a list of integer values generated by this layer. These may be interpreted as temperatures, rainfall
     * amounts, or biomeList[] indices based on the particular GenLayer subclass.
     */
    public int[] getInts(int par1, int par2, int par3, int par4)
    {
        int[] aint = this.parent.getInts(par1, par2, par3, par4);
        int[] aint1 = IntCache.getIntCache(par3 * par4);

        for (int i1 = 0; i1 < par4; ++i1)
        {
            for (int j1 = 0; j1 < par3; ++j1)
            {
                this.initChunkSeed((long)(j1 + par1), (long)(i1 + par2));
                int k1 = aint[j1 + i1 * par3];
                int l1 = (k1 & 3840) >> 8;
                k1 &= -3841;

                if (isOceanic(k1))
                {
                    aint1[j1 + i1 * par3] = k1;
                }
                else if (k1 == Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND))
                {
                    aint1[j1 + i1 * par3] = k1;
                }
                else if (k1 == 1)
                {
                    if (l1 > 0)
                    {
                        if (this.nextInt(3) == 0)
                        {
                            aint1[j1 + i1 * par3] = Biome.getIdForBiome(Biomes.MESA_CLEAR_ROCK);
                        }
                        else
                        {
                            aint1[j1 + i1 * par3] = Biome.getIdForBiome(Biomes.MESA_ROCK);
                        }
                    }
                    else
                    {
                        aint1[j1 + i1 * par3] = Biome.getIdForBiome(this.field_151623_c[this.nextInt(this.field_151623_c.length)]);
                    }
                }
                else if (k1 == 2)
                {
                    if (l1 > 0)
                    {
                        aint1[j1 + i1 * par3] = Biome.getIdForBiome(Biomes.JUNGLE);
                    }
                    else
                    {
                        aint1[j1 + i1 * par3] = Biome.getIdForBiome(this.field_151621_d[this.nextInt(this.field_151621_d.length)]);
                    }
                }
                else if (k1 == 3)
                {
                    if (l1 > 0)
                    {
                        aint1[j1 + i1 * par3] = Biome.getIdForBiome(Biomes.REDWOOD_TAIGA);
                    }
                    else
                    {
                        aint1[j1 + i1 * par3] = Biome.getIdForBiome(this.field_151622_e[this.nextInt(this.field_151622_e.length)]);
                    }
                }
                else if (k1 == 4)
                {
                    aint1[j1 + i1 * par3] = Biome.getIdForBiome(this.field_151620_f[this.nextInt(this.field_151620_f.length)]);
                }
                else
                {
                    aint1[j1 + i1 * par3] = Biome.getIdForBiome(Biomes.MUSHROOM_ISLAND);
                }
                logger.info("("+(i1+par2)+","+(j1+par1)+") Climate "+k1 + " " + aint[j1 + i1 * par3]+" Biome " + aint1[j1 + i1 * par3]);
            }
        }

        return aint1;
    }
}