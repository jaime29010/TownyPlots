package me.jaimemartz.townyplots;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class TownBlockDiscovery {
    private final Set<TownBlock> blocks = new HashSet<>();
    private final TownBlock initial;

    public TownBlockDiscovery(TownBlock initial) {
        this.initial = initial;
        WorldCoord coord = initial.getWorldCoord();

        TownBlock upper = TownyUtils.getBlock(coord.add(1, 0));
        TownBlock lower = TownyUtils.getBlock(coord.add(-1, 0));

        TownBlock left = TownyUtils.getBlock(coord.add(0, -1));
        TownBlock right = TownyUtils.getBlock(coord.add(0, 1));

        if (upper != null && upper.isForSale()) {
            blocks.add(upper);
            if (left != null && left.isForSale()) {
                blocks.add(left);
                TownBlock corner = TownyUtils.getBlock(coord.add(1, -1));
                if (corner != null && corner.isForSale()) {
                    blocks.add(corner);
                }
            } else if (right != null && right.isForSale()) {
                blocks.add(right);
                TownBlock corner = TownyUtils.getBlock(coord.add(1, 1));
                if (corner != null && corner.isForSale()) {
                    blocks.add(corner);
                }
            }
        } else if (lower != null && lower.isForSale()) {
            blocks.add(lower);
            if (left != null && left.isForSale()) {
                blocks.add(left);
                TownBlock corner = TownyUtils.getBlock(coord.add(-1, -1));
                if (corner != null && corner.isForSale()) {
                    blocks.add(corner);
                }
            } else if (right != null && right.isForSale()) {
                blocks.add(right);
                TownBlock corner = TownyUtils.getBlock(coord.add(-1, 1));
                if (corner != null && corner.isForSale()) {
                    blocks.add(corner);
                }
            }
        }
    }

    private static TownBlock[] getAdjacentBlocks(TownBlock block) {
        WorldCoord coord = block.getWorldCoord();
        return new TownBlock[] {
                TownyUtils.getBlock(coord.add(1, 0)),
                TownyUtils.getBlock(coord.add(-1, 0)),
                TownyUtils.getBlock(coord.add(0, 1)),
                TownyUtils.getBlock(coord.add(0, -1))
        };
    }

    public TownBlock getInitial() {
        return initial;
    }

    public Set<TownBlock> getBlocks() {
        return blocks;
    }

    public Set<WorldCoord> getWorldCoords() {
        return blocks.stream().map(TownBlock::getWorldCoord).collect(Collectors.toSet());
    }
}
