package com.exiledradio.buzzybeesbackport.honey;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;

/** The honeycomb block: decorative, hardness 0.6, coral block sounds (1.15's values). */
public class BlockHoneycomb extends Block {

    public BlockHoneycomb() {
        super(Material.CLAY, MapColor.ADOBE);
        setHardness(0.6F);
        setSoundType(HoneyContent.CORAL);
    }
}
