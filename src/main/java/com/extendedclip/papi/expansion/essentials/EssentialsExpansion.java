/*
 *
 * Essentials-Expansion
 * Copyright (C) 2019 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.essentials;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.DescParseTickFormat;
import com.earth2me.essentials.utils.NumberUtil;
import com.google.common.primitives.Ints;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.essentialsx.api.v2.services.BalanceTop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

public class EssentialsExpansion extends PlaceholderExpansion {

    private String k;
    private String m;
    private String b;
    private String t;
    private String q;

    private final DecimalFormat coordsFormat = new DecimalFormat("#.###");

    private Essentials essentials;
    private BalanceTop baltop;

    private final String VERSION = getClass().getPackage().getImplementationVersion();

    @Override
    public boolean canRegister() {
        return Bukkit.getPluginManager().isPluginEnabled("Essentials");
    }

    @Override
    public boolean register() {
        k = getString("formatting.thousands", "k");
        m = getString("formatting.millions", "m");
        b = getString("formatting.billions", "b");
        t = getString("formatting.trillions", "t");
        q = getString("formatting.quadrillions", "q");

        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials != null && essentials.isEnabled()) {
            baltop = essentials.getBalanceTop();
            baltop.calculateBalanceTopMapAsync();
            return super.register();
        }
        return false;
    }

    @Override
    public @NotNull String getAuthor() {
        return "clip";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "essentials";
    }

    @Override
    public @NotNull String getVersion() {
        return VERSION;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        final String papiTrue = PlaceholderAPIPlugin.booleanTrue();
        final String papiFalse = PlaceholderAPIPlugin.booleanFalse();

        // Put this before the null check as most of it is not required
        if (params.startsWith("baltop_")) {
            Map<UUID, BalanceTop.Entry> baltopCache = baltop.getBalanceTopCache();
            params = params.substring(7);

            if (params.startsWith("balance_")) {
                params = params.substring(8);

                if (params.startsWith("fixed_")) {
                    params = params.substring(6);

                    Integer id = Ints.tryParse(params);
                    if (id == null) {
                        return "Invalid ID";
                    }

                    BalanceTop.Entry[] entries = baltopCache.values().toArray(new BalanceTop.Entry[0]);
                    if (id >= entries.length) {
                        return "0";
                    }
                    return String.valueOf(entries[id].getBalance().longValue());
                }

                if (params.startsWith("formatted_")) {
                    params = params.substring(10);

                    Integer id = Ints.tryParse(params);
                    if (id == null) {
                        return "Invalid ID";
                    }

                    BalanceTop.Entry[] entries = baltopCache.values().toArray(new BalanceTop.Entry[0]);
                    if (id >= entries.length) {
                        return "0";
                    }
                    return fixMoney(entries[id].getBalance().doubleValue());
                }

                if (params.startsWith("commas_")) {
                    params = params.substring(7);

                    Integer id = Ints.tryParse(params);
                    if (id == null) {
                        return "Invalid ID";
                    }

                    BalanceTop.Entry[] entries = baltopCache.values().toArray(new BalanceTop.Entry[0]);
                    if (id >= entries.length) {
                        return "0";
                    }
                    return NumberUtil.formatAsPrettyCurrency(BigDecimal.valueOf(entries[id].getBalance().doubleValue()));
                }

                Integer id = Ints.tryParse(params);
                if (id == null) {
                    return "Invalid ID";
                }

                BalanceTop.Entry[] entries = baltopCache.values().toArray(new BalanceTop.Entry[0]);
                if (id >= entries.length) {
                    return "0";
                }
                return String.valueOf(entries[id].getBalance().doubleValue());
            }

            if (params.startsWith("player_")) {
                params = params.substring(7);

                boolean stripped = false;

                if (params.startsWith("stripped_")) {
                    params = params.substring(9);
                    stripped = true;
                }

                Integer id = Ints.tryParse(params);
                if (id == null) {
                    return "Invalid ID";
                }

                BalanceTop.Entry[] entries = baltopCache.values().toArray(new BalanceTop.Entry[0]);
                if (id >= entries.length) {
                    return "0";
                }

                if (stripped) {
                    User user = essentials.getUser(entries[id].getUuid());
                    if (user != null) {
                        return user.getName();
                    } else {
                        return null;
                    }
                } else {
                    return entries[id].getDisplayName();
                }
            }

            if (params.equals("rank")) {
                // Another null check because it is above the normal one
                if (offlinePlayer == null) return "";

                if (!baltopCache.containsKey(offlinePlayer.getUniqueId())) {
                    return "";
                }

                return String.valueOf(new ArrayList<>(baltopCache.keySet()).indexOf(offlinePlayer.getUniqueId()) + 1);
            }

            return null;
        }

        if (offlinePlayer == null) return "";
        final User user = essentials.getUser(offlinePlayer.getUniqueId());

        if (params.equals("tp_cooldown")) {
            final double cooldown = essentials.getSettings().getTeleportCooldown();
            final long d1 = System.currentTimeMillis();
            final long d2 = user.getLastTeleportTimestamp();
            long diff = TimeUnit.MILLISECONDS.toSeconds(d1 - d2);
            if (diff < cooldown) return String.valueOf((int) (cooldown - diff));
            return "0";
        }

        if (params.startsWith("kit_last_use_")) {
            String kitName = params.split("kit_last_use_")[1].toLowerCase();
            Kit kit;

            try {
                kit = new Kit(kitName, essentials);
            } catch (Exception e) {
                return "Invalid kit name";
            }

            long time = user.getKitTimestamp(kit.getName());

            if (time == 1 || time <= 0) {
                return "1";
            }
            return PlaceholderAPIPlugin.getDateFormat().format(new Date(time));
        }

        if (params.startsWith("kit_is_available_")) {
            String kitName = params.split("kit_is_available_")[1].toLowerCase();
            Kit kit;
            long time;

            try {
                kit = new Kit(kitName, essentials);
            } catch (Exception e) {
                return "Invalid kit name";
            }

            try {
                time = kit.getNextUse(user);
            } catch (Exception e) {
                return papiFalse;
            }

            return time == 0 ? papiTrue : papiFalse;
        }

        if (params.startsWith("kit_time_until_available_")) {
            String kitName = params.split("kit_time_until_available_")[1].toLowerCase();
            boolean raw = false;
            Kit kit;
            long time;

            if (kitName.startsWith("raw_")) {
                raw = true;
                kitName = kitName.substring(4);

                if (kitName.isEmpty()) {
                    return "Invalid kit name";
                }
            }

            try {
                kit = new Kit(kitName, essentials);
            } catch (Exception e) {
                return "Invalid kit name";
            }

            try {
                time = kit.getNextUse(user);
            } catch (Exception e) {
                return "-1";
            }

            if (time <= System.currentTimeMillis()) {
                return raw ? "0" : DateUtil.formatDateDiff(System.currentTimeMillis());
            }

            if (raw) {
                return String.valueOf(Instant.now().until(Instant.ofEpochMilli(time), ChronoUnit.MILLIS));
            } else {
                return DateUtil.formatDateDiff(time);
            }
        }

        if (params.startsWith("has_kit_")) {
            Player player = offlinePlayer.getPlayer();
            if (player == null) return papiFalse;

            String kit = params.split("has_kit_")[1];
            return player.hasPermission("essentials.kits." + kit) ? papiTrue : papiFalse;
        }

        if (params.startsWith("home_")) {
            Integer homeNumber;

            // Removes all the letters from the identifier to get the home slot.
            // Checks if the number slot is an integer or not.
            if ((homeNumber = Ints.tryParse(params.replaceAll("\\D+", ""))) == null) return null;

            // Since it is easier for users to type from 1-x I subtract one from the original number to work from 0-x.
            homeNumber -= 1;

            // checks if the home is out of bounds and returns and empty string if it is.
            if (homeNumber >= user.getHomes().size() || homeNumber < 0) return "";

            // checks if the identifier matches the pattern home_%d
            if (params.matches("(\\w+_)(\\d+)")) return user.getHomes().get(homeNumber);

            //checks if the identifier matches the pattern home_%d_(w/x/y/z)
            if (params.matches("(\\w+_)(\\d+)(_\\w)")) {

                try {
                    final Location home = user.getHome(user.getHomes().get(homeNumber));
                    final StringBuilder stringBuilder = new StringBuilder();

                    switch (params.charAt(params.length() - 1)) {
                        case 'w' -> stringBuilder.append(Objects.requireNonNull(home.getWorld()).getName());
                        case 'x' -> stringBuilder.append(coordsFormat.format(home.getX()));
                        case 'y' -> stringBuilder.append((int) home.getY());
                        case 'z' -> stringBuilder.append(coordsFormat.format(home.getZ()));
                    }

                    return stringBuilder.toString();
                } catch (Exception e) {
                    return null;
                }
            }
        }

        if (params.startsWith("worth")) {
            ItemStack item;

            if (params.contains(":")) {
                Material material = Material.getMaterial(params.replace("worth:", "").toUpperCase());

                if (material == null) return "";
                item = new ItemStack(material, 1);
            } else {
                Player oPlayer = offlinePlayer.getPlayer();
                if (oPlayer == null) return "";

                if (oPlayer.getInventory().getItemInMainHand().getType() == Material.AIR) return "";
                item = oPlayer.getInventory().getItemInMainHand();
            }

            BigDecimal worth = essentials.getWorth().getPrice(essentials, item);
            if (worth == null) return "";
            return String.valueOf(worth.doubleValue());
        }

        return switch (params) {
            case "is_clearinventory_confirm" -> user.isPromptingClearConfirm() ? papiTrue : papiFalse;
            case "is_pay_confirm" -> user.isPromptingPayConfirm() ? papiTrue : papiFalse;
            case "is_pay_enabled" -> user.isAcceptingPay() ? papiTrue : papiFalse;
            case "is_teleport_enabled" -> user.isTeleportEnabled() ? papiTrue : papiFalse;
            case "is_muted" -> user.isMuted() ? papiTrue : papiFalse;
            case "vanished" -> user.isVanished() ? papiTrue : papiFalse;
            case "afk" -> user.isAfk() ? papiTrue : papiFalse;
            case "afk_reason" -> user.getAfkMessage() == null ? "" : ChatColor.translateAlternateColorCodes('&', user.getAfkMessage());
            case "afk_player_count" -> String.valueOf(essentials.getUsers().getAllUserUUIDs().stream()
                    .map(UUID -> essentials.getUser(UUID))
                    .filter(User::isAfk)
                    .count());
            case "msg_ignore" -> user.isIgnoreMsg() ? papiTrue : papiFalse;
            case "fly" -> user.getBase().getAllowFlight() ? papiTrue : papiFalse;
            case "nickname" -> user.getNickname() != null ? user.getNickname() : offlinePlayer.getName();
            case "nickname_stripped" -> ChatColor.stripColor(user.getNickname() != null ? user.getNickname() : offlinePlayer.getName());
            case "muted_time_remaining" -> user.isMuted() ? DateUtil.formatDateDiff(user.getMuteTimeout()) : "";
            case "geolocation" -> user.getGeoLocation() != null ? user.getGeoLocation() : "";
            case "godmode" -> user.isGodModeEnabled() ? papiTrue : papiFalse;
            case "unique" -> NumberFormat.getInstance().format(essentials.getUsers().getUserCount());
            case "homes_set" -> user.getHomes().isEmpty() ? "0" : String.valueOf(user.getHomes().size());
            case "homes_max" -> String.valueOf(essentials.getSettings().getHomeLimit(user));
            case "jailed" -> user.isJailed() ? papiTrue : papiFalse;
            case "jailed_time_remaining" -> user.isJailed() ? user.getFormattedJailTime() : "";
            case "pm_recipient" -> user.getReplyRecipient() != null ? user.getReplyRecipient().getName() : "";
            case "safe_online" -> String.valueOf(StreamSupport.stream(essentials.getOnlineUsers().spliterator(), false)
                    .filter(user1 -> !user1.isHidden())
                    .count());
            case "world_date" -> DateFormat.getDateInstance(DateFormat.MEDIUM, essentials.getI18n().getCurrentLocale())
                    .format(DescParseTickFormat.ticksToDate(user.getWorld() == null ? 0 : user.getWorld().getFullTime()));
            case "world_time" -> DescParseTickFormat.format12(user.getWorld() == null ? 0 : user.getWorld().getTime());
            case "world_time_24" -> DescParseTickFormat.format24(user.getWorld() == null ? 0 : user.getWorld().getTime());
            case "balance" -> String.valueOf(user.getMoney().doubleValue());
            case "balance_formatted" -> NumberUtil.formatAsPrettyCurrency(BigDecimal.valueOf(user.getMoney().doubleValue()));
            default -> null;
        };
    }

    private String format(double d) {
        NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
        format.setMaximumFractionDigits(2);
        format.setMinimumFractionDigits(0);
        return format.format(d);
    }

    private String fixMoney(double d) {

        if (d < 1000L) {
            return format(d);
        }
        if (d < 1000000L) {
            return format(d / 1000L) + k;
        }
        if (d < 1000000000L) {
            return format(d / 1000000L) + m;
        }
        if (d < 1000000000000L) {
            return format(d / 1000000000L) + b;
        }
        if (d < 1000000000000000L) {
            return format(d / 1000000000000L) + t;
        }
        if (d < 1000000000000000000L) {
            return format(d / 1000000000000000L) + q;
        }

        return String.valueOf(d);
    }
}
