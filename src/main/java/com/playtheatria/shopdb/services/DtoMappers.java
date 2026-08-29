package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.ChestShopRow;
import com.playtheatria.shopdb.database.PlayerRepository;
import com.playtheatria.shopdb.database.RegionRow;
import com.playtheatria.shopdb.models.ChestShopDto;
import com.playtheatria.shopdb.models.ChestShopPlayerDto;
import com.playtheatria.shopdb.models.ChestShopRegionDto;
import com.playtheatria.shopdb.models.Location;
import com.playtheatria.shopdb.models.PlayerDto;
import com.playtheatria.shopdb.models.PlayerRegionDto;
import com.playtheatria.shopdb.models.RegionDto;
import com.playtheatria.shopdb.models.RegionPlayerDto;
import com.playtheatria.shopdb.models.Server;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class DtoMappers {
    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    public static ChestShopDto toChestShopDto(ChestShopRow row) {
        ChestShopDto dto = new ChestShopDto();
        dto.setServer(row.server == null ? null : Server.valueOf(row.server));
        dto.setLocation(new Location(row.x, row.y, row.z));
        dto.setMaterial(row.material);
        dto.setBaseMaterial(row.baseMaterial);
        if (row.itemDetails != null) {
            try {
                com.playtheatria.shopdb.models.ItemDetailsDto details = GSON.fromJson(row.itemDetails,
                        com.playtheatria.shopdb.models.ItemDetailsDto.class);
                if (details != null) {
                    // Search-only metadata is used by repository queries and
                    // autocomplete, not rendered or exposed as item tooltip data.
                    details.setSearchEnchants(null);
                }
                dto.setItemDetails(details);
            } catch (RuntimeException ignored) {
                // Imported databases may contain one malformed legacy value.
                // Keep the shop usable; metadata search already ignores it.
            }
        }

        // The old DTO's getOwner() lazily created an empty object, so "owner" is
        // always present in the JSON — {} when the shop has no resolvable owner.
        ChestShopPlayerDto owner = new ChestShopPlayerDto();
        owner.setName(row.ownerName);
        dto.setOwner(owner);

        if (row.townName != null) {
            ChestShopRegionDto town = new ChestShopRegionDto();
            town.setName(row.townName);
            dto.setTown(town);
        }

        dto.setQuantity(row.quantity);
        dto.setQuantityAvailable(row.quantityAvailable);
        dto.setBuyPrice(row.buyPrice);
        dto.setSellPrice(row.sellPrice);
        dto.setBuyPriceEach(row.buyPriceEach);
        dto.setSellPriceEach(row.sellPriceEach);
        dto.setFull(row.isFull);
        dto.setBuySign(row.isBuySign);
        dto.setSellSign(row.isSellSign);
        return dto;
    }

    public static PlayerDto toPlayerDto(PlayerRepository.PlayerRow player, int numChestShops, List<RegionRow> towns) {
        PlayerDto dto = new PlayerDto();
        dto.setName(player.name);
        dto.setNumChestShops(numChestShops);

        List<PlayerRegionDto> townDtos = new ArrayList<>();
        for (RegionRow town : towns) {
            PlayerRegionDto townDto = new PlayerRegionDto();
            townDto.setName(town.name);
            townDto.setServer(town.server == null ? null : Server.valueOf(town.server));
            townDtos.add(townDto);
        }
        dto.setTowns(townDtos);
        return dto;
    }

    public static RegionDto toRegionDto(RegionRow region, int numChestShops, List<String> mayorNames) {
        RegionDto dto = new RegionDto();
        dto.setName(region.name);
        dto.setServer(region.server == null ? null : Server.valueOf(region.server));
        dto.setiBounds(new Location(region.iX, region.iY, region.iZ));
        dto.setoBounds(new Location(region.oX, region.oY, region.oZ));
        dto.setNumChestShops(numChestShops);
        dto.setActive(region.active);

        List<RegionPlayerDto> mayors = new ArrayList<>();
        for (String name : mayorNames) {
            RegionPlayerDto mayor = new RegionPlayerDto();
            mayor.setName(name);
            mayors.add(mayor);
        }
        dto.setMayors(mayors);

        dto.setLastUpdated(region.lastUpdated == null ? null : new Timestamp(region.lastUpdated));
        return dto;
    }

    private DtoMappers() {
    }
}
