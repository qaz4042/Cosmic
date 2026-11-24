package server;

import client.Character;
import client.inventory.InventoryType;

/** Restricted second-job respec policy. No job-advancement rewards. */
public final class AdventurerJobSwitch {
    private AdventurerJobSwitch() {}

    public static boolean branch(int job) {
        return job >= 100 && job < 600 && job % 10 <= 2 && switch (job / 10 * 10) {
            case 110,120,130,210,220,230,310,320,410,420,510,520 -> true;
            default -> false;
        };
    }

    public static int branchRoot(int job) { return job / 10 * 10; }
    public static int targetJob(int source, int selectedBranch) {
        if (!branch(source) || !branch(selectedBranch) || selectedBranch % 10 != 0
                || source / 100 != selectedBranch / 100) return -1;
        return selectedBranch + source % 10;
    }

    public static boolean sameFamily(int source, int target) {
        return branch(source) && branch(target) && source / 100 == target / 100
                && source % 10 == target % 10;
    }

    public static boolean canSwapStats(int source, int target) {
        return sameFamily(source, target) && source != target && source / 100 == 5;
    }

    public static boolean branchSkill(int skill) {
        return branch(skill / 10000);
    }

    // Stable status codes used by the skill-window button: ready, combat, unsupported, state, equipment.
    public static int status(Character player) {
        if (!branch(player.getJob().getId()) || player.getLevel() < 30) return 2;
        if (!player.isLoggedinWorld() || !player.isAlive() || player.isChangingMaps()
                || player.getMap() == null
                || player.getEventInstance() != null || player.getMap().getEventInstance() != null
                || player.getTrade() != null || player.getShop() != null
                || player.getPlayerShop() != null || player.getMiniGame() != null
                || player.getHiredMerchant() != null || player.getCashShop().isOpened()) return 3;
        if (player.getJobSwitchQuietSeconds() > 0) return 1;
        if (!JobSwitchEquipment.hasRoom(player)) return 4;
        return 0;
    }

    public static String rejection(Character player, int target) {
        if (!sameFamily(player.getJob().getId(), target))
            return "仅支持冒险家同一大类内的分支互换（保持当前转数）。";
        if (target == player.getJob().getId()) return "你已经是这个职业。";
        return switch (status(player)) {
            case 1 -> "请先脱战 30 秒，还需 " + player.getJobSwitchQuietSeconds() + " 秒。";
            case 2 -> "仅开放 30 级以上冒险家分支互换（保持当前转数）。";
            case 3 -> "请先结束交易或活动。";
            case 4 -> "装备栏空间不足，无法自动卸下全部装备，请先整理背包。";
            default -> null;
        };
    }

    public static int refundedSp(int job, int remaining, java.util.Map<Integer, Integer> skills) {
        if (!branch(job) || remaining < 0) throw new IllegalArgumentException("职业或 SP 异常");
        long result = remaining;
        for (var entry : skills.entrySet()) {
            int owner = entry.getKey() / 10000;
            int level = entry.getValue();
            if (level < 0 || (level > 0 && owner != 0 && owner != job / 100 * 100 && !(branch(owner) && branchRoot(owner) == branchRoot(job) && owner % 10 <= job % 10)))
                throw new IllegalArgumentException("存在其他职业技能，请先由管理员检查角色");
            if (branch(owner) && branchRoot(owner) == branchRoot(job)) result += level;
        }
        if (result > Short.MAX_VALUE) throw new IllegalArgumentException("SP 超出范围");
        return (int) result;
    }
}
