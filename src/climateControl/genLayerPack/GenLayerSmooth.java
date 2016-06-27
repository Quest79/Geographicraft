package climateControl.genLayerPack;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;

public class GenLayerSmooth extends GenLayerPack
{
    private static final String __OBFID = "CL_00000569";
    private boolean onlyBiomes = false;

    public GenLayerSmooth(long par1, GenLayer par3GenLayer)
    {
        super(par1);
        super.parent = par3GenLayer;
    }

    public GenLayerSmooth(long par1, GenLayer par3GenLayer,boolean onlyBiomes)
    {
        super(par1);
        super.parent = par3GenLayer;
        this.onlyBiomes = onlyBiomes;
    }
    /**
     * Returns a list of integer values generated by this layer. These may be interpreted as temperatures, rainfall
     * amounts, or biomeList[] indices based on the particular GenLayer subclass.
     */
    public int[] getInts(int par1, int par2, int par3, int par4)
    {
        int i1 = par1 - 1;
        int j1 = par2 - 1;
        int k1 = par3 + 2;
        int l1 = par4 + 2;
        int[] aint = this.parent.getInts(i1, j1, k1, l1);
        int[] aint1 = IntCache.getIntCache(par3 * par4);
        taste(aint,k1*l1);
        poison(aint1,par3*par4);
        taste(aint,k1*l1);

        for (int i2 = 0; i2 < par4; i2++)
        {
            for (int j2 = 0; j2 < par3; j2++)
            {
                int k2 = aint[j2 + 0 + (i2 + 1) * k1];
                int l2 = aint[j2 + 2 + (i2 + 1) * k1];
                int i3 = aint[j2 + 1 + (i2 + 0) * k1];
                int j3 = aint[j2 + 1 + (i2 + 2) * k1];
                int k3 = aint[j2 + 1 + (i2 + 1) * k1];

                if (k2 == l2 && i3 == j3)
                {
                    this.initChunkSeed((long)(j2 + par1), (long)(i2 + par2));

                    if (this.nextInt(2) == 0)
                    {
                        k3 = k2;
                    }
                    else
                    {
                        k3 = i3;
                    }
                }
                else
                {
                    if (k2 == l2)
                    {
                        k3 = k2;
                    }

                    if (i3 == j3)
                    {
                        k3 = i3;
                    }
                }
                if (k3<0) throw new RuntimeException("i2 "+i2 + " j2 "+j2 + " k2 " + k2 + " l2 " + l2 +
                        " i3 " + i3 + " j3 " + j3 + " k3 " + k3 + " orig "+ aint[j2 + 1 + (i2 + 1) * k1]);

                aint1[j2 + i2 * par3] = k3;
                if (onlyBiomes) {
                    if (k3>255) throw new RuntimeException("i2 "+i2 + " j2 "+j2 + " k2 " + k2 + " l2 " + l2 +
                        " i3 " + i3 + " j3 " + j3 + " k3 " + k3 + " orig "+ aint[j2 + 1 + (i2 + 1) * k1]);
                }
            }
        }
        if (onlyBiomes) {
            for (int i = 0; i < par3 * par4;i++) {
                if (aint1[i]>255) throw new RuntimeException();
            }
        }
        taste(aint1,par3*par4);
        return aint1;
    }
}