package top.mrxiaom.crazyauctions.reloaded.util.enums;

import org.bukkit.Bukkit;

/**
 * In the Plus version, a better method has been used for Version-related operations.
 */
public enum Version {
    
    TOO_OLD(-1),
    v1_7_R1(171), v1_7_R2(172), v1_7_R3(173), v1_7_R4(174),
    v1_8_R1(181), v1_8_R2(182), v1_8_R3(183),
    v1_9_R1(191), v1_9_R2(192),
    v1_10_R1(1101),
    v1_11_R1(1111),
    v1_12_R1(1121),
    v1_13_R2(1132),
    v1_14_R1(1141),
    TOO_NEW(-2);
    
    public static Version currentVersion;
    private static Version latest;
    private final Integer versionInteger;
    
    Version(int versionInteger) {
        this.versionInteger = versionInteger;
    }
    
    /**
     *
     * @return Get the server's Minecraft version.
     */
    public static Version getCurrentVersion() {
        if (currentVersion == null) {
            String ver = Bukkit.getServer().getClass().getPackage().getName();
            int v = Integer.parseInt(ver.substring(ver.lastIndexOf('.') + 1).replace("_", "").replace("R", "").replace("v", ""));
            for (Version version : values()) {
                if (version.getVersionInteger() == v) {
                    currentVersion = version;
                    break;
                }
            }
            if (v > Version.getLatestVersion().getVersionInteger()) {
                currentVersion = Version.getLatestVersion();
            }
            if (currentVersion == null) {
                currentVersion = Version.TOO_NEW;
            }
        }
        return currentVersion;
    }
    
    /**
     * Get the latest version allowed by the Version class.
     * @return The latest version.
     */
    public static Version getLatestVersion() {
        if (latest == null) {
            Version v = Version.TOO_OLD;
            for (Version version : values()) {
                if (version.comparedTo(v) == 1) {
                    v = version;
                }
            }
            return v;
        } else {
            return latest;
        }
    }
    
    /**
     *
     * @return The server's minecraft version as an integer.
     */
    public Integer getVersionInteger() {
        return this.versionInteger;
    }
    
    /**
     * This checks if the current version is older, newer, or is the checked version.
     * @param version The version you are checking.
     * @return -1 if older, 0 if the same, and 1 if newer.
     */
    public Integer comparedTo(Version version) {
        int result = -1;
        int current = this.getVersionInteger();
        int check = version.getVersionInteger();
        if (current > check || check == -2) {// check is newer then current
            result = 1;
        } else if (current == check) {// check is the same as current
            result = 0;
        }
        return result;
    }
    
    /**
     * Checks to see if the current version is newer then the checked version.
     * @param version The version you are checking.
     * @return True if newer then the checked version and false if the same or older.
     */
    public Boolean isNewer(Version version) {
        return this.versionInteger > version.versionInteger || this.versionInteger == -2;
    }
    
    /**
     * Checks to see if the current version is the same as the checked version.
     * @param version The version you are checking.
     * @return True if both the current and checked version is the same and false if otherwise.
     */
    public Boolean isSame(Version version) {
        return this.versionInteger.equals(version.versionInteger);
    }
    
    /**
     * Checks to see if the current version is older than the checked version.
     * @param version The version you are checking.
     * @return True if older than the checked version and false if the same or newer.
     */
    public Boolean isOlder(Version version) {
        return this.versionInteger < version.versionInteger || this.versionInteger == -1;
    }
    
}