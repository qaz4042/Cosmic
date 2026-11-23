// NPC selection is advisory; the server revalidates family, state and SP at confirmation.
var status = 0;
var target = 0;
var originalJob = 0;
var swapStats = false;
var expectedStr = 0;
var expectedDex = 0;
var jobs = [];
var names = [];
var targetName = "";
var groups = [
    [[110,120,130], ["剑客","准骑士","枪战士"]],
    [[210,220,230], ["火毒法师","冰雷法师","牧师"]],
    [[310,320], ["猎人","弩弓手"]],
    [[410,420], ["刺客","侠客"]],
    [[510,520], ["拳手","火枪手"]]
];

function start() {
    originalJob = cm.getJobId();
    for (var i = 0; i < groups.length; i++) {
        if (groups[i][0].indexOf(originalJob) >= 0) {
            jobs = groups[i][0]; names = groups[i][1]; break;
        }
    }
    if (jobs.length == 0 || cm.getLevel() < 30 || cm.getLevel() >= 70) {
        cm.sendOk("仅开放 30–69 级冒险家同一大类内的二转分支互换。");
        cm.dispose(); return;
    }
    var text = "冒险家二转职业切换（30–69级）\r\n请先在普通城镇卸下全部装备。\r\n保留一转技能、等级和基础血蓝；清除二转技能并退还实际投入的 SP，不发转职奖励。解除自身全部增益，清理旧二转技能快捷键、相关宏与挂机配置。\r\n";
    for (var j = 0; j < jobs.length; j++)
        if (jobs[j] != originalJob) text += "#L" + j + "#" + names[j] + "#l\r\n";
    cm.sendSimple(text);
}

function confirm() {
    expectedStr = cm.getPlayer().getStr();
    expectedDex = cm.getPlayer().getDex();
    status = 2;
    var stats = swapStats
        ? "力量 " + expectedStr + " → " + expectedDex + "，敏捷 " + expectedDex + " → " + expectedStr
        : "保留当前全部属性加点";
    cm.sendYesNo("确认切换为 #b" + targetName + "#k？\r\n" + stats
        + "。\r\n原二转技能加点将清除并退还 SP，不保留各分支独立加点方案。历史升级血蓝不重算。请自行准备目标武器及箭矢／飞镖／子弹，切换后重新加点并检查快捷键。");
}

function action(mode, type, selection) {
    if (mode != 1) { cm.dispose(); return; }
    if (status == 0) {
        if (selection < 0 || selection >= jobs.length || selection != Math.floor(selection)
                || jobs[selection] == originalJob) { cm.dispose(); return; }
        target = jobs[selection]; targetName = names[selection];
        if (originalJob == 510 || originalJob == 520) {
            status = 1;
            cm.sendSimple("拳手主力量，火枪手主敏捷。请选择属性处理方式：\r\n#L0#保留当前力量、敏捷#l\r\n#L1#互换力量与敏捷（总点数、智力、运气及未用 AP 不变）#l");
        } else confirm();
    } else if (status == 1) {
        if (selection !== 0 && selection !== 1) { cm.dispose(); return; }
        swapStats = selection == 1;
        confirm();
    } else if (status == 2) {
        status = 3;
        var result = cm.getPlayer().switchAdventurerBranch(target, originalJob, swapStats, expectedStr, expectedDex);
        cm.sendOk(result);
        cm.dispose();
    }
}
