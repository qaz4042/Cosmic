package server;

import client.Character;
import client.inventory.InventoryType;

/** Restricted second-job respec policy. No job-advancement rewards. */
public final class AdventurerJobSwitch {
    private AdventurerJobSwitch() {}

    public static boolean branch(int job) {
        return switch (job) {
            case 110, 120, 130, 210, 220, 230, 310, 320, 410, 420, 510, 520 -> true;
            default -> false;
        };
    }

    public static boolean sameFamily(int source, int target) {
        return branch(source) && branch(target) && source / 100 == target / 100;
    }

    public static boolean canSwapStats(int source, int target) {
        return sameFamily(source, target) && source != target && source / 100 == 5;
    }

    public static boolean branchSkill(int skill) {
        return branch(skill / 10000);
    }

    public static String rejection(Character player, int target) {
        if (!sameFamily(player.getJob().getId(), target))
            return "仅支持冒险家同一大类内的二转分支互换。";
        if (target == player.getJob().getId()) return "你已经是这个职业。";
        if (player.getLevel() < 30 || player.getLevel() >= 70)
            return "仅开放 30–69 级冒险家二转分支互换。";
        if (!player.isLoggedinWorld() || !player.isAlive() || player.isChangingMaps()
                || player.getMap() == null || !player.getMap().isTown()
                || player.getEventInstance() != null || player.getMap().getEventInstance() != null
                || player.getTrade() != null || player.getShop() != null
                || player.getPlayerShop() != null || player.getMiniGame() != null
                || player.getHiredMerchant() != null || player.getCashShop().isOpened())
            return "请在普通城镇、存活且无交易或活动的状态下切换。";
        if (!player.getInventory(InventoryType.EQUIPPED).list().isEmpty())
            return "请先卸下全部装备（含现金装备），再切换职业。";
        return null;
    }

    public static int refundedSp(int job, int remaining, java.util.Map<Integer, Integer> skills) {
        if (!branch(job) || remaining < 0) throw new IllegalArgumentException("职业或 SP 异常");
        long result = remaining;
        for (var entry : skills.entrySet()) {
            int owner = entry.getKey() / 10000;
            int level = entry.getValue();
            if (level < 0 || (level > 0 && owner != 0 && owner != job / 100 * 100 && owner != job))
                throw new IllegalArgumentException("存在其他职业技能，请先由管理员检查角色");
            if (owner == job) result += level;
        }
        if (result > Short.MAX_VALUE) throw new IllegalArgumentException("SP 超出范围");
        return (int) result;
    }
}
